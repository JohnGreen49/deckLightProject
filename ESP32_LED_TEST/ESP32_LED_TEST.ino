// ESP32 + WS2812B diagnostic with single-pixel walker.
//
// Strip is confirmed WS2812B / NEOPIXEL / GRB (cut from a working reel).
// Symptom being investigated: first frame latches, subsequent frames are
// ignored. The most common cause on ESP32 + WS2812B is a marginal 3.3V
// data line driving a 5V-logic strip whose "high" threshold is ~3.5V.
//
// Hardware fixes to try (best-first):
//   1. Add a 330-470 ohm resistor in series with DIN, close to the ESP32.
//      Improves edge integrity and often resolves the marginal-threshold
//      lockup outright.
//   2. Keep the data wire SHORT (< 15 cm) and well away from power wires.
//   3. If 1-2 don't fix it: add a 74AHCT125 level shifter (3.3V -> 5V).
//   4. Trick that often works on small builds: power the first LED through
//      a 1N4001 diode, dropping its VDD ~0.6V so its logic threshold falls
//      to ~3.1V, which 3.3V can comfortably meet.
//
// Wiring (3-pad single-wire family):
//   LED +5V  -> board 5V (or external 5V for longer strips, common ground)
//   LED GND  -> board GND  (MUST share ground)
//   LED DIN  -> GPIO defined by DATA_PIN (with 330-470 ohm in series)

#include <FastLED.h>

#define CHIPSET     WS2812B
#define COLOR_ORDER GRB

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
