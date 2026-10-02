package com.exonoxic.palimpsest.client.dev;

import com.exonoxic.palimpsest.Palimpsest;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.Timer;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Vector3f;
import org.slf4j.Logger;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Development-only recording for {@link Trailer}. While recording, the game runs in lockstep: the
 * client runs exactly one tick every {@link #FRAMES_PER_TICK} rendered frames (the frames between
 * are drawn at even partial ticks) and the integrated server one tick per client tick, so the video
 * plays back at true speed however slowly the frames are drawn. Every frame is piped raw into
 * ffmpeg, and every sound the game starts is written to a log with the frame it started on, its
 * file, loudness and pan, for {@code tools/trailer_mix.py} to mix the soundtrack from afterwards
 * (the machine that renders it has no sound card).
 */
@Mod.EventBusSubscriber(modid = Palimpsest.MODID, value = Dist.CLIENT)
final class TrailerRecorder {
    private static final Logger LOG = LogUtils.getLogger();
    static final int FRAMES_PER_TICK = Math.max(1, Integer.getInteger("palimpsest.trailer.framesPerTick", 2));
    static final int FPS = 20 * FRAMES_PER_TICK;

    private static final Semaphore SERVER_TICKS = new Semaphore(0);
    private static volatile boolean lockstep;
    private static boolean recording;
    private static int phase;
    private static long frames;
    private static Path dir;
    private static Process ffmpeg;
    private static OutputStream video;
    private static PrintWriter log;

    private TrailerRecorder() {}

    /** Swaps in the lockstep timer and opens the sound log. */
    static void install(Minecraft mc) throws IOException, ReflectiveOperationException {
        dir = mc.gameDirectory.toPath().resolve("trailer");
        Files.createDirectories(dir);
        log = new PrintWriter(Files.newBufferedWriter(dir.resolve("sounds.tsv")), true);
        log.printf(Locale.ROOT, "meta\tfps\t%d%n", FPS);
        for (Field f : Minecraft.class.getDeclaredFields()) {
            if (f.getType() != Timer.class || Modifier.isStatic(f.getModifiers())) continue;
            f.setAccessible(true);
            f.set(mc, new LockstepTimer());
            LOG.info("[trailer] lockstep timer installed, {} frames per tick", FRAMES_PER_TICK);
            return;
        }
        throw new NoSuchFieldException("Minecraft has no Timer field");
    }

    static boolean isRecording() {
        return recording;
    }

    static long frames() {
        return frames;
    }

    static void record(boolean on) {
        recording = on;
        lockstep = on;
        phase = 0;
        if (!on) SERVER_TICKS.drainPermits();
    }

    /** A marker for the mixer (music in and out, hits), at the current frame. */
    static void cue(String what) {
        if (log != null) log.printf(Locale.ROOT, "cue\t%d\t%s%n", frames, what);
        LOG.info("[trailer] cue {} at frame {}", what, frames);
    }

    static void finish() {
        record(false);
        if (log != null) {
            log.printf(Locale.ROOT, "meta\tframes\t%d%n", frames);
            log.close();
        }
        try {
            if (video != null) video.close();
            if (ffmpeg != null && !ffmpeg.waitFor(5, TimeUnit.MINUTES)) LOG.error("[trailer] ffmpeg did not finish");
            else if (ffmpeg != null) LOG.info("[trailer] ffmpeg exited {}", ffmpeg.exitValue());
        } catch (IOException | InterruptedException e) {
            LOG.error("[trailer] could not close the video", e);
        }
        LOG.info("[trailer] {} frames at {} fps ({} s)", frames, FPS, String.format(Locale.ROOT, "%.1f", frames / (double) FPS));
    }

    // ------------------------------------------------------------------ lockstep

    /** Ticks once every {@link #FRAMES_PER_TICK} calls while recording, otherwise keeps real time. */
    private static final class LockstepTimer extends Timer {
        LockstepTimer() {
            super(20.0F, 0L);
        }

        @Override
        public int advanceTime(long millis) {
            int real = super.advanceTime(millis);
            if (!lockstep) return real;
            partialTick = phase / (float) FRAMES_PER_TICK;
            int ticks = phase == 0 ? 1 : 0;
            phase = (phase + 1) % FRAMES_PER_TICK;
            return ticks;
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START && lockstep) SERVER_TICKS.release();
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        try {
            while (lockstep && !SERVER_TICKS.tryAcquire(50, TimeUnit.MILLISECONDS)) {
                // Waiting for the client to draw its frames.
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------ frames

    @SubscribeEvent
    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !recording) return;
        Minecraft mc = Minecraft.getInstance();
        NativeImage image = Screenshot.takeScreenshot(mc.getMainRenderTarget());
        try {
            if (ffmpeg == null) start(image.getWidth(), image.getHeight());
            int[] pixels = image.getPixelsRGBA();
            ByteBuffer bytes = ByteBuffer.allocate(pixels.length * 4).order(ByteOrder.LITTLE_ENDIAN);
            bytes.asIntBuffer().put(pixels);
            video.write(bytes.array());
            frames++;
            if (frames % 400 == 0) LOG.info("[trailer] {} frames", frames);
        } catch (IOException e) {
            LOG.error("[trailer] TRAILER_FAIL writing frame {}", frames, e);
            recording = false;
        } finally {
            image.close();
        }
    }

    private static void start(int width, int height) throws IOException {
        LOG.info("[trailer] recording {}x{} at {} fps", width, height, FPS);
        ffmpeg = new ProcessBuilder("ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgba",
                "-s", width + "x" + height, "-framerate", Integer.toString(FPS), "-i", "-",
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "12", "-pix_fmt", "yuv420p", dir.resolve("frames.mp4").toString())
                .redirectOutput(ProcessBuilder.Redirect.INHERIT).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        video = new BufferedOutputStream(ffmpeg.getOutputStream(), 1 << 22);
    }

    // ------------------------------------------------------------------ sounds

    @SubscribeEvent
    public static void onSound(PlaySoundEvent event) {
        SoundInstance s = event.getSound();
        if (!recording || log == null || s == null || s.getSource() == SoundSource.MUSIC || s.getSource() == SoundSource.RECORDS) return;
        Minecraft mc = Minecraft.getInstance();
        // Resolving picks a variant; the engine resolves again, but nothing here is heard anyway.
        WeighedSoundEvents events = s.resolve(mc.getSoundManager());
        Sound sound = s.getSound();
        if (events == null || sound == null || sound == SoundManager.EMPTY_SOUND) return;
        float volume = s.getVolume();
        double gain = Math.min(1.0D, volume);
        double pan = 0.0D;
        if (!s.isRelative() && s.getAttenuation() == SoundInstance.Attenuation.LINEAR) {
            Camera camera = mc.gameRenderer.getMainCamera();
            Vec3 to = new Vec3(s.getX(), s.getY(), s.getZ()).subtract(camera.getPosition());
            double reach = Math.max(1.0F, volume) * sound.getAttenuationDistance();
            gain *= Math.max(0.0D, 1.0D - to.length() / reach);
            if (to.length() > 0.5D) {
                Vector3f left = camera.getLeftVector();
                pan = -(to.x * left.x() + to.y * left.y() + to.z * left.z()) / to.length();
            }
        }
        if (gain < 0.01D) return;
        log.printf(Locale.ROOT, "snd\t%d\t%s\t%.3f\t%.3f\t%.2f\t%b%n", frames, sound.getPath(), gain, s.getPitch(), pan, s.isLooping());
    }
}
