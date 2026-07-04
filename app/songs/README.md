# Test songs

A set of small songs for exercising each waveform and effect in isolation, plus a
few public-domain melodies that combine them.

## Running

From the project root:

```bash
./gradlew run --args="songs/wave_sine.txt"
```

Swap in any file below. (The `application` plugin in `build.gradle.kts` provides the
`run` task.)

## File format recap

```
sampleRate beatsPerMeasure tempo          # header: three positive integers
wave effect$arg... | measure | measure |  # one line per channel
```

- **waves:** `sin`, `square`, `saw`, `whitenoise`
- **effects:** `vol$level` (>=0), `tanh$drive` (>0), `clip$threshold` (>0),
  `ads$attack$decay$sustain` (attack in seconds >=0, decay >= attack, sustain in [0,1])
- **measure:** space-separated `note duration` pairs; durations are in beats and every
  measure must sum to `beatsPerMeasure`. Notes are scientific pitch (`C4` = middle C,
  `A0`–`C8`, sharps `C#`/flats `Db`); `-` is a rest.

## Waveforms in isolation

| File | What it exercises |
|------|-------------------|
| `wave_sine.txt`   | `sin` — C-major scale up and down, no effects |
| `wave_square.txt` | `square` — same scale, hollow/buzzy tone |
| `wave_saw.txt`    | `saw` — same scale, bright/brassy tone |
| `wave_noise.txt`  | `whitenoise` — pitch is ignored; bursts and rests then a sustained tail |

## Effects in isolation (clean `sin` + one effect)

| File | What it exercises |
|------|-------------------|
| `fx_volume.txt`    | `vol$0.3` — the scale at reduced amplitude |
| `fx_tanh.txt`      | `tanh$10` — soft saturation/overdrive |
| `fx_clip.txt`      | `clip$0.3` — hard clipping distortion |
| `fx_ads_pluck.txt` | `ads$0$.15$0` — instant attack, fast decay to silence (percussive pluck) |
| `fx_ads_pad.txt`   | `ads$.4$.8$.7` — slow attack swell, sustained pad (slower tempo) |

## Stacked effects

| File | What it exercises |
|------|-------------------|
| `fx_tanh_clip_stack.txt` | `saw tanh$8 clip$0.6` — two effects chained into an overdriven lead (plus a clean bass) |

Effects are applied left to right, each wrapping the previous, so `tanh$8 clip$0.6`
means `clip(tanh(saw))`: the saw is soft-saturated by `tanh`, then its peaks are
hard-flattened by `clip`. Compare against `fx_tanh.txt` and `fx_clip.txt` to hear each
stage on its own. Reversing the order (`clip$0.6 tanh$8`) gives a different tone.

## Public-domain melodies

| File | What it exercises |
|------|-------------------|
| `twinkle_twinkle.txt`        | two channels (sin melody + soft square bass) — mixing |
| `ode_to_joy.txt`             | saw lead + square bass, dotted rhythms |
| `mary_had_a_little_lamb.txt` | single square channel with a plucky envelope |
| `jingle_bells.txt`           | melody + a `whitenoise` hi-hat pattern (percussion) |
| `frere_jacques_round.txt`    | two-voice canon (second voice enters two measures late) — timing/overlap |
| `amazing_grace.txt`          | 3/4 time (`beatsPerMeasure = 3`) with a padded pickup measure |
