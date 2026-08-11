/**
 * A ringtone, generated in the browser.
 *
 * Same approach as the alarm sounds: no audio file to download, so this works
 * with no signal. The pattern imitates the standard Indian ringback tone —
 * a warble that rings for about a second, then pauses.
 *
 * Deliberately not a copy of any particular phone's default ringtone. It needs
 * to sound like a phone ringing, not like a specific handset, and imitating a
 * real one too closely is both unnecessary and legally murky.
 */

let context = null;

function audioContext() {
    if (!context) {
        const Ctor = window.AudioContext || window.webkitAudioContext;
        context = new Ctor();
    }
    if (context.state === "suspended") {
        context.resume();
    }
    return context;
}

/**
 * Starts ringing. Returns a function that stops it.
 *
 * Two sine tones at 400Hz and 450Hz beating together give the warbling
 * quality of a telephone ring; a plain single tone sounds like a test signal.
 */
export function startRinging() {
    const ctx = audioContext();

    const a = ctx.createOscillator();
    const b = ctx.createOscillator();
    const gain = ctx.createGain();

    a.type = "sine";
    b.type = "sine";
    a.frequency.value = 400;
    b.frequency.value = 450;

    a.connect(gain);
    b.connect(gain);
    gain.connect(ctx.destination);

    const now = ctx.currentTime;
    gain.gain.setValueAtTime(0, now);

    // Ring for 0.4s, gap 0.2s, ring 0.4s, then two seconds of silence.
    // Scheduled far ahead so it keeps going without a JavaScript timer.
    for (let i = 0; i < 200; i++) {
        const cycle = now + i * 3;
        ring(gain, cycle);
        ring(gain, cycle + 0.6);
    }

    a.start(now);
    b.start(now);

    return function stop() {
        const t = ctx.currentTime;
        gain.gain.cancelScheduledValues(t);
        gain.gain.setValueAtTime(0, t);
        try {
            a.stop(t + 0.05);
            b.stop(t + 0.05);
        } catch {
            // Already stopped.
        }
    };
}

/** One burst of ring, with soft edges so it does not click. */
function ring(gain, at) {
    gain.gain.setValueAtTime(0, at);
    gain.gain.linearRampToValueAtTime(0.35, at + 0.02);
    gain.gain.setValueAtTime(0.35, at + 0.38);
    gain.gain.linearRampToValueAtTime(0, at + 0.4);
}

/**
 * Vibrates the phone in time with the ring, where supported.
 *
 * Returns a function that cancels it. Silently does nothing on desktop, which
 * is fine — this is a mobile feature.
 */
export function startVibrating() {
    if (!navigator.vibrate) {
        return () => {};
    }

    const pattern = [400, 200, 400, 2000];
    navigator.vibrate(pattern);

    const interval = setInterval(() => navigator.vibrate(pattern), 3000);

    return function stop() {
        clearInterval(interval);
        navigator.vibrate(0);
    };
}
