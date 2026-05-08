// ESP32 + WS2812 diagnostic sketch.
//
// Wiring checklist for a 9-LED strip:
//   LED VCC  -> 5V (preferably a SEPARATE 5V supply on long strips or
//                   when the board is USB-powered; see power note below)
//   LED GND  -> ESP32 GND  (MUST share ground)
//   LED DIN  -> ESP32 GPIO defined by DATA_PIN, into the FIRST pixel's DIN
//
// Power note (HiLetgo UNO D1 R32 / WROOM-32 boards over USB):
//   USB delivers ~500mA. The ESP32 itself burns ~250-400mA with WiFi up, so
//   the strip is left with maybe 100-250mA. 9 WS2812s at full white want
//   ~540mA, which sags the 5V rail and locks the strip after the first frame
//   (symptom: LEDs latch one pattern at reset and never update again).
//   Either lower BRIGHTNESS (this sketch) or feed the strip from a separate
//   5V supply with a common ground.
//
// Pin notes:
//   - GPIO 16 is safe on plain WROOM-32 boards. WROVER boards use GPIO 16/17
//     for PSRAM and they will not drive LEDs cleanly there.
//   - WS2812 data is nominally 5V logic. ESP32's 3.3V output usually works on
//     short runs; on marginal strips add a 74AHCT125 level shifter.
//   - If your strip is WS2811/SK6812/WS2813, change LED_TYPE accordingly.

#include <FastLED.h>

#define NUM_LEDS    9
#define DATA_PIN    16
#define LED_TYPE    WS2812
#define COLOR_ORDER GRB
#define BRIGHTNESS  16    // low to keep total current well under USB budget

CRGB leds[NUM_LEDS];

static void showAll(CRGB c, uint16_t holdMs, const char *label) {
  Serial.print("show: "); Serial.print(label); Serial.print(" ... ");
  fill_solid(leds, NUM_LEDS, c);
  FastLED.show();
  Serial.println("done");
  delay(holdMs);
}

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println();
  Serial.println("=== ESP32 LED diagnostic ===");
  Serial.print("LEDs: ");      Serial.println(NUM_LEDS);
  Serial.print("Data pin: GPIO "); Serial.println(DATA_PIN);
  Serial.print("Brightness: "); Serial.println(BRIGHTNESS);

  FastLED.addLeds<LED_TYPE, DATA_PIN, COLOR_ORDER>(leds, NUM_LEDS);
  FastLED.setBrightness(BRIGHTNESS);
  FastLED.clear(true);

  // Long solid holds make it impossible to miss if the strip is alive.
  showAll(CRGB::Red,   3000, "all RED 3s");
  showAll(CRGB::Green, 3000, "all GREEN 3s");
  showAll(CRGB::Blue,  3000, "all BLUE 3s");
  showAll(CRGB::White, 2000, "all WHITE 2s");
  FastLED.clear(true);
  Serial.println("Entering rainbow loop. If you only see colors here and not");
  Serial.println("during the solid holds above, the strip is fine and we have");
  Serial.println("a brightness/timing issue elsewhere.");
}

void loop() {
  static uint8_t hue = 0;
  static uint32_t lastBeat = 0;
  uint32_t now = millis();

  fill_rainbow(leds, NUM_LEDS, hue, 255 / NUM_LEDS);
  FastLED.show();
  hue++;

  // Heartbeat once per second so we know the loop is running even if no light.
  if (now - lastBeat >= 1000) {
    lastBeat = now;
    Serial.print("alive t="); Serial.print(now / 1000);
    Serial.print("s hue="); Serial.println(hue);
  }
  delay(30);
}
