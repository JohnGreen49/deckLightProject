// ESP32 + addressable LED strip diagnostic / CHIPSET PROBE.
//
// The "separate purchase" 9-LED test strip has an unknown chipset. WS2812B
// (800 kHz) and WS2811 (400 kHz) use different bit timing; sending one to
// the other produces exactly the "looks frozen on a frame, never updates"
// failure mode.
//
// HOW TO USE THIS SKETCH:
//   1. Set CHIPSET below to one option.
//   2. Flash. Watch for: a single bright pixel walking left-to-right,
//      cycling RED -> GREEN -> BLUE per pass, with serial "alive" lines.
//   3. If you see clean motion -> that's your chipset. Done.
//      If you see frozen / wrong-color / garbled output -> try the next
//      option. Most likely culprits are WS2811, then SK6812, then WS2812B.
//
// Wiring (all options, 3-pad single-wire family):
//   LED +5V  -> board 5V (use external 5V supply for >20 LEDs at brightness)
//   LED GND  -> board GND  (MUST share ground)
//   LED DIN  -> GPIO defined by DATA_PIN, into the FIRST pixel's DIN
//
// COLOR_ORDER is the second clue. If motion works but the "RED" pass
// shows green or blue, swap the order (GRB / RGB / BRG / etc.) and reflash.

#include <FastLED.h>

// --- pick ONE chipset to test -----------------------------------------------
// WS2811   : 400 kHz, common in 12mm bullet pixels and many cheap strips.
// WS2812B  : 800 kHz, classic NeoPixel 5050.
// WS2812   : 800 kHz, original (slightly looser timing than WS2812B).
// SK6812   : 800 kHz, WS2812B-like; works as RGB if 3 channels.
// TM1809   : 800 kHz, older clone.
// UCS1903  : 400 kHz, older clone.
#define CHIPSET     WS2811
#define COLOR_ORDER RGB     // try GRB if reds/greens look swapped
// ----------------------------------------------------------------------------

#define NUM_LEDS    9
#define DATA_PIN    16
#define BRIGHTNESS  32

CRGB leds[NUM_LEDS];

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println();
  Serial.println("=== ESP32 LED chipset probe ===");
  Serial.print("LEDs: ");          Serial.println(NUM_LEDS);
  Serial.print("Data pin: GPIO "); Serial.println(DATA_PIN);
  Serial.print("Brightness: ");    Serial.println(BRIGHTNESS);
  // Echo the active chipset/color order so the serial log matches the build.
  #define _STR(x)  #x
  #define _XSTR(x) _STR(x)
  Serial.print("Chipset: ");      Serial.println(_XSTR(CHIPSET));
  Serial.print("Color order: ");  Serial.println(_XSTR(COLOR_ORDER));

  FastLED.addLeds<CHIPSET, DATA_PIN, COLOR_ORDER>(leds, NUM_LEDS);
  FastLED.setBrightness(BRIGHTNESS);
  FastLED.clear(true);
}

// Single-pixel walker is the clearest "is the strip actually receiving
// frames" test: any one frozen LED or scrambled pattern means the chipset
// or color order is wrong.
void loop() {
  static const CRGB cycleColors[] = { CRGB::Red, CRGB::Green, CRGB::Blue };
  static uint8_t pos = 0;
  static uint8_t colorIdx = 0;
  static uint32_t lastBeat = 0;

  fill_solid(leds, NUM_LEDS, CRGB::Black);
  leds[pos] = cycleColors[colorIdx];
  FastLED.show();

  uint32_t now = millis();
  if (now - lastBeat >= 1000) {
    lastBeat = now;
    Serial.print("alive t="); Serial.print(now / 1000);
    Serial.print("s pos=");   Serial.print(pos);
    Serial.print(" color=");  Serial.println(colorIdx);
  }

  pos++;
  if (pos >= NUM_LEDS) {
    pos = 0;
    colorIdx = (colorIdx + 1) % 3;
  }
  delay(250);
}
