// ESP32 + WS2812B per-LED ramp test.
//
// Strip is confirmed WS2812B / NEOPIXEL / GRB.
//
// For each LED 0..N-1 in sequence:
//   - ramp RED   from 0 to 255 over 1024 ms (others off)
//   - ramp GREEN from 0 to 255 over 1024 ms
//   - ramp BLUE  from 0 to 255 over 1024 ms
//   - return that LED to black, advance to next
// Then loop.
//
// Why this is the right diagnostic now:
//   - A slow ramp must visibly change every ~16 ms. If the strip ever
//     freezes on a non-zero value, you can see the exact frame it died.
//   - One-LED-at-a-time isolates a possible bad pixel / addressing issue.
//   - Iterating R, G, B exposes color-order or per-channel data corruption.

#include <FastLED.h>

#define NUM_LEDS    9
#define DATA_PIN    16
#define CHIPSET     WS2812B
#define COLOR_ORDER GRB
#define BRIGHTNESS  64        // master scale; ramp value runs 0..255 underneath
#define RAMP_MS     1024UL

CRGB leds[NUM_LEDS];

// Refresh roughly every 16 ms (~60 Hz). Faster wastes show() calls; slower
// makes the ramp look stepped.
#define FRAME_MS    16

static void rampLed(uint8_t pos, uint8_t channel, const char *label) {
  Serial.print("LED "); Serial.print(pos);
  Serial.print(" ramp "); Serial.println(label);

  uint32_t start = millis();
  uint32_t nextFrame = start;
  uint8_t  lastV = 0;

  while (true) {
    uint32_t now = millis();
    uint32_t elapsed = now - start;
    if (elapsed >= RAMP_MS) break;

    if ((int32_t)(now - nextFrame) >= 0) {
      nextFrame += FRAME_MS;
      uint8_t v = (uint8_t)((elapsed * 255UL) / RAMP_MS);
      if (v != lastV) {
        fill_solid(leds, NUM_LEDS, CRGB::Black);
        switch (channel) {
          case 0: leds[pos] = CRGB(v, 0, 0); break;
          case 1: leds[pos] = CRGB(0, v, 0); break;
          case 2: leds[pos] = CRGB(0, 0, v); break;
        }
        FastLED.show();
        lastV = v;
      }
    } else {
      delay(1);
    }
  }

  // End each ramp at full value briefly so the peak is visible, then black
  // before moving on.
  fill_solid(leds, NUM_LEDS, CRGB::Black);
  switch (channel) {
    case 0: leds[pos] = CRGB(255, 0, 0); break;
    case 1: leds[pos] = CRGB(0, 255, 0); break;
    case 2: leds[pos] = CRGB(0, 0, 255); break;
  }
  FastLED.show();
  delay(150);

  fill_solid(leds, NUM_LEDS, CRGB::Black);
  FastLED.show();
}

void setup() {
  Serial.begin(115200);
  delay(300);
  Serial.println();
  Serial.println("=== ESP32 LED ramp test ===");
  Serial.print("LEDs: ");          Serial.println(NUM_LEDS);
  Serial.print("Data pin: GPIO "); Serial.println(DATA_PIN);
  Serial.print("Brightness: ");    Serial.println(BRIGHTNESS);

  FastLED.addLeds<CHIPSET, DATA_PIN, COLOR_ORDER>(leds, NUM_LEDS);
  FastLED.setBrightness(BRIGHTNESS);
  FastLED.clear(true);
}

void loop() {
  for (uint8_t i = 0; i < NUM_LEDS; i++) {
    rampLed(i, 0, "RED");
    rampLed(i, 1, "GREEN");
    rampLed(i, 2, "BLUE");
  }
  Serial.println("--- full pass complete, restarting ---");
}
