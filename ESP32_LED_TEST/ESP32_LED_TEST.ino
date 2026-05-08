// ESP32 + WS2812 diagnostic sketch.
//
// Wiring checklist for a 9-LED strip:
//   LED VCC  -> 5V (USB 5V is fine for 9 LEDs)
//   LED GND  -> ESP32 GND  (MUST share ground)
//   LED DIN  -> ESP32 GPIO defined by DATA_PIN, into the FIRST pixel's DIN
//
// Notes:
//   - GPIO 16 is safe on classic ESP32, ESP32-S3, and ESP32-C3 (not a strapping pin).
//     The previous default of GPIO 5 is a strapping pin on classic ESP32 and can
//     cause boot oddities; avoid it for LED data.
//   - WS2812 data is nominally 5V logic. ESP32's 3.3V output usually works on short
//     runs, but if signal is marginal try a 74AHCT125 level shifter or power the
//     first LED from 3.7-4.2V so its logic threshold drops.
//   - If your strip is WS2811/SK6812/WS2813, change LED_TYPE accordingly.

#include <FastLED.h>

#define NUM_LEDS    9
#define DATA_PIN    16
#define LED_TYPE    WS2812
#define COLOR_ORDER GRB
#define BRIGHTNESS  255   // full brightness for diagnostic visibility

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
