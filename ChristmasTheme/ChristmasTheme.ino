#include <FastLED.h>

#define NUM_LEDS 600
#define DATA_PIN 6
#define NUM_SPARKS 10
#define NUM_BOXES 5
#define BACK_COLOR CRGB(16,0,0)

CRGB leds[NUM_LEDS];
int cycle=0;
int numChars = 40;
bool newData=false;
char receivedChars[40];

class Spark {
  public:
    int pos;
    int age = 0;
    CRGB color;
};

Spark sparks[NUM_SPARKS];
boolean halted = false;

void setup() {
	FastLED.addLeds<NEOPIXEL, DATA_PIN>(leds, NUM_LEDS);
  Serial.begin(1200);
  
  // Setup initial sparks
  for (int i= 0; i < NUM_SPARKS; i++) {
    Spark &s = sparks[i];
    s.pos = random(NUM_LEDS);
    s.age = random(250);
    s.color = CRGB::White;
  };
}

void loop() {
  cycle ++;
  
  if (Serial.available() > 0) {
       readCommand();
  }
  if (halted) return;
  background();
  updateSparks();
	FastLED.show();
}

void readCommand()
{
  static byte ndx = 0;
  char endMarker = '\n';
  char rc;
 
  while (Serial.available() > 0) {
      rc = Serial.read();
      if (rc != endMarker) {
         Serial.print(rc);
         receivedChars[ndx] = rc;
         ndx ++;
      }
      else
      {
         receivedChars[ndx] = 0;
         ndx = 0;
         String cmd = String(receivedChars);
         if (cmd == "stop")
         {
             Serial.print(" - Stopped\n");
             halted = true;
        }
        if (cmd == "go")
        {
             Serial.print(" - Started\n");
             halted = false;
         }
      }
  }
}

void background() {
  fill_solid(leds, NUM_LEDS, BACK_COLOR );
}
  
void updateSparks() {
  for (int i=1 ; i <= NUM_SPARKS-1; i++) {
    Spark &s = sparks[i];
    s.age++;
    if (s.age > 250)
    {
      s.pos = random(NUM_LEDS);
      s.age = 0;
      s.color = CRGB::White;
    }
    else
    {
         s.color.red = s.color.red - 1;
         s.color.green = s.color.green - 1;
         s.color.blue = s.color.blue - 1;
    }
    leds[s.pos] = s.color;
  };
}

