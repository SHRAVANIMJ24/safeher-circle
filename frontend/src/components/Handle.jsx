/**
 * A pseudonym, rendered the same way everywhere.
 *
 * The colour is derived by hashing the handle text, so "calm-maple-3867" is
 * always the same shade without the server storing anything extra. Monospace
 * is deliberate: it reads as a generated identifier rather than a name, which
 * is exactly what it is.
 */
export default function Handle({ handle }) {
    const hue = hueFor(handle);
    return (
        <span className="handle" style={{ "--handle-hue": hue }}>
            {handle}
        </span>
    );
}

function hueFor(text) {
    if (!text) return 0;
    let hash = 0;
    for (let i = 0; i < text.length; i++) {
        hash = (hash * 31 + text.charCodeAt(i)) % 360;
    }
    return hash;
}
