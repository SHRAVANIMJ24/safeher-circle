/**
 * Alarm sounds, generated in the browser with the Web Audio API.
 *
 * Nothing is downloaded, so an alarm still works with no signal — which is
 * exactly when someone is most likely to need it. Each sound is built from
 * oscillators and gain curves rather than a recording.
 *
 * Browsers refuse to play audio until the user has interacted with the page,
 * so the context is created on first use (a button press) rather than at load.
 */

let context = null;

function audioContext() {
    if (!context) {
        const Ctor = window.AudioContext || window.webkitAudioContext;
        context = new Ctor();
    }
    // Browsers suspend the context when a tab is backgrounded.
    if (context.state === "suspended") {
        context.resume();
    }
    return context;
}

/**
 * A rising and falling wail, like an emergency vehicle.
 *
 * The pitch sweep is what makes a siren carry. A steady tone at the same
 * volume is far easier for the ear to filter out as background noise.
 */
function createSiren(ctx, destination) {
    const oscillator = ctx.createOscillator();
    const gain = ctx.createGain();

    oscillator.type = "sawtooth";
    oscillator.connect(gain);
    gain.connect(destination);
    gain.gain.value = 0.6;

    const now = ctx.currentTime;
    oscillator.frequency.setValueAtTime(600, now);

    // Sweep between 600Hz and 1400Hz on a one-second cycle, scheduled well
    // ahead so the pattern continues without any JavaScript timer running.
    for (let i = 0; i < 600; i++) {
        const t = now + i;
        oscillator.frequency.linearRampToValueAtTime(1400, t + 0.5);
        oscillator.frequency.linearRampToValueAtTime(600, t + 1.0);
    }

    oscillator.start(now);
    return [oscillator];
}

/**
 * A harsh pulsing alarm, like a smoke detector.
 *
 * Two slightly detuned square waves beat against each other, producing a rough
 * tone that is hard to ignore and hard to mistake for a ringtone.
 */
function createPulse(ctx, destination) {
    const a = ctx.createOscillator();
    const b = ctx.createOscillator();
    const gain = ctx.createGain();

    a.type = "square";
    b.type = "square";
    a.frequency.value = 2730;
    b.frequency.value = 2760;

    a.connect(gain);
    b.connect(gain);
    gain.connect(destination);

    const now = ctx.currentTime;
    gain.gain.setValueAtTime(0, now);

    // Sharp on-off pulses, four per second.
    for (let i = 0; i < 1200; i++) {
        const t = now + i * 0.25;
        gain.gain.setValueAtTime(0.5, t);
        gain.gain.setValueAtTime(0, t + 0.12);
    }

    a.start(now);
    b.start(now);
    return [a, b];
}

/**
 * A slow, low warble.
 *
 * Low frequencies lose less energy passing through doors and floors, so this
 * is the one to use when the point is being heard in the next room rather
 * than across a street.
 */
function createLowWarble(ctx, destination) {
    const oscillator = ctx.createOscillator();
    const modulator = ctx.createOscillator();
    const modulatorGain = ctx.createGain();
    const gain = ctx.createGain();

    oscillator.type = "sawtooth";
    oscillator.frequency.value = 180;

    modulator.type = "sine";
    modulator.frequency.value = 6;
    modulatorGain.gain.value = 60;

    modulator.connect(modulatorGain);
    modulatorGain.connect(oscillator.frequency);

    oscillator.connect(gain);
    gain.connect(destination);
    gain.gain.value = 0.65;

    const now = ctx.currentTime;
    oscillator.start(now);
    modulator.start(now);
    return [oscillator, modulator];
}

const BUILDERS = {
    SIREN: createSiren,
    PULSE: createPulse,
    LOW: createLowWarble,
};

export const ALARM_TYPES = [
    {
        id: "SIREN",
        label: "Siren",
        description: "A rising wail that carries a long way outdoors.",
    },
    {
        id: "PULSE",
        label: "Pulse",
        description: "A sharp repeating tone, like a smoke alarm.",
    },
    {
        id: "LOW",
        label: "Low warble",
        description: "A deep sound that travels through walls and doors.",
    },
];

/**
 * Starts an alarm. Returns a function that stops it.
 *
 * The caller holds the stop function rather than this module tracking state,
 * so a component that unmounts can always silence what it started.
 */
export function playAlarm(type = "SIREN") {
    const ctx = audioContext();
    const master = ctx.createGain();

    master.gain.value = 1;
    master.connect(ctx.destination);

    const build = BUILDERS[type] || BUILDERS.SIREN;
    const nodes = build(ctx, master);

    return function stop() {
        const now = ctx.currentTime;
        // Fade rather than a hard cut, which produces an unpleasant click.
        master.gain.setValueAtTime(master.gain.value, now);
        master.gain.linearRampToValueAtTime(0, now + 0.08);
        nodes.forEach((node) => {
            try {
                node.stop(now + 0.1);
            } catch {
                // Already stopped; nothing to do.
            }
        });
    };
}

/** Plays a sound briefly, so someone can hear it before they rely on it. */
export function previewAlarm(type, milliseconds = 1500) {
    const stop = playAlarm(type);
    setTimeout(stop, milliseconds);
    return stop;
}
