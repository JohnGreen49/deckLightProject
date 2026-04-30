
#include <FastLED.h>
#define NUM_LEDS 9
#define DATA_PIN 7

CRGB leds[NUM_LEDS];
int seed = 123;

void setup() {
  FastLED.addLeds<NEOPIXEL, DATA_PIN>(leds, NUM_LEDS);
  reset();
}

void loop() {

  for (int i = 0; i < NUM_LEDS; i++) {
    leds[i] = CRGB::Red;
    FastLED.show();
    delay(1000);
    leds[i] = CRGB::Blue;
    FastLED.show();
    delay(1000);
    leds[i] = CRGB::Green;
    FastLED.show();
    delay(1000);
  }
}

void reset() {
  for (int dot = NUM_LEDS; dot > 0; dot--) {
    leds[dot] = 0;
  }
}
