// ESP32 + WS2812 cross-check using Adafruit_NeoPixel.
//
// Purpose: independent driver from FastLED. If this sketch shows the
// solid color holds correctly but the FastLED sketch does not, the
// problem is FastLED/RMT timing on ESP32 Arduino core 3.x. If this
// sketch fails identically, the problem is electrical (signal level,
// power, ground, wrong chipset).
//
// Install: Arduino IDE -> Library Manager -> "Adafruit NeoPixel".
// Wire: same pins as ESP32_LED_TEST (DATA_PIN = GPIO 16).

#include <Adafruit_NeoPixel.h>

#define NUM_LEDS  9
#define DATA_PIN  16

Adafruit_NeoPixel strip(NUM_LEDS, DATA_PIN, NEO_GRB + NEO_KHZ800);

static void showAll(uint8_t r, uint8_t g, uint8_t b, uint16_t holdMs, const char *label) {
  Serial.print("show: "); Serial.print(label); Serial.print(" ... ");
  for (int i = 0; i < NUM_LEDS; i++) strip.setPixelColor(i, strip.Color(r, g, b));
  strip.show();
  Serial.println("done");
  delay(holdMs);
}

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println();
  Serial.println("=== ESP32 NeoPixel cross-check ===");
  Serial.print("LEDs: ");          Serial.println(NUM_LEDS);
  Serial.print("Data pin: GPIO "); Serial.println(DATA_PIN);

  strip.begin();
  // Low brightness keeps total strip current well under the USB budget on
  // boards like the HiLetgo UNO D1 R32. If the strip locks after one frame
  // at higher brightness, it's a power sag, not a driver bug.
  strip.setBrightness(16);
  strip.clear();
  strip.show();

  showAll(255,   0,   0, 3000, "all RED 3s");
  showAll(  0, 255,   0, 3000, "all GREEN 3s");
  showAll(  0,   0, 255, 3000, "all BLUE 3s");
  showAll(255, 255, 255, 2000, "all WHITE 2s");

  // Single-pixel walk to expose ordering / dead-pixel issues.
  Serial.println("Single-pixel walk (white) ...");
  for (int i = 0; i < NUM_LEDS; i++) {
    strip.clear();
    strip.setPixelColor(i, strip.Color(255, 255, 255));
    strip.show();
    Serial.print("  pixel "); Serial.println(i);
    delay(500);
  }
  strip.clear();
  strip.show();
}

void loop() {
  static uint16_t hue = 0;
  static uint32_t lastBeat = 0;
  uint32_t now = millis();

  for (int i = 0; i < NUM_LEDS; i++) {
    uint16_t h = hue + (i * 65536UL / NUM_LEDS);
    strip.setPixelColor(i, strip.gamma32(strip.ColorHSV(h)));
  }
  strip.show();
  hue += 256;

  if (now - lastBeat >= 1000) {
    lastBeat = now;
    Serial.print("alive t="); Serial.print(now / 1000); Serial.println("s");
  }
  delay(30);
}
