// ESP32 + WS2812 (NeoPixel) hardware validation.
// 1) Brief R/G/B/white startup sweep so a dead pixel or wrong NUM_LEDS is obvious.
// 2) Continuous rainbow cycle across the strip.

#include <FastLED.h>

#define NUM_LEDS    9
#define DATA_PIN    5
#define LED_TYPE    WS2812
#define COLOR_ORDER GRB
#define BRIGHTNESS  64

CRGB leds[NUM_LEDS];

static void showAll(CRGB c, uint16_t holdMs) {
  fill_solid(leds, NUM_LEDS, c);
  FastLED.show();
  delay(holdMs);
}

static void walk(CRGB c, uint16_t stepMs) {
  for (int i = 0; i < NUM_LEDS; i++) {
    fill_solid(leds, NUM_LEDS, CRGB::Black);
    leds[i] = c;
    FastLED.show();
    delay(stepMs);
  }
}

void setup() {
  Serial.begin(115200);
  delay(200);
  Serial.println();
  Serial.println("=== ESP32 LED test ===");
  Serial.print("LEDs: "); Serial.println(NUM_LEDS);
  Serial.print("Data pin: GPIO "); Serial.println(DATA_PIN);

  FastLED.addLeds<LED_TYPE, DATA_PIN, COLOR_ORDER>(leds, NUM_LEDS);
  FastLED.setBrightness(BRIGHTNESS);
  FastLED.clear(true);

  Serial.println("Startup sweep...");
  walk(CRGB::Red,   150);
  walk(CRGB::Green, 150);
  walk(CRGB::Blue,  150);
  showAll(CRGB::White, 500);
  FastLED.clear(true);
  Serial.println("Rainbow cycle running.");
}

void loop() {
  static uint8_t hue = 0;
  fill_rainbow(leds, NUM_LEDS, hue, 255 / NUM_LEDS);
  FastLED.show();
  hue++;
  delay(30);
}
