#include <SPI.h>
#include <Ethernet.h>
#include <PubSubClient.h>
#include <FastLED.h>

#define NUM_LEDS 9
#define DATA_PIN 7
#define SD_CS_PIN 4
#define W5100_CS_PIN 10

byte mac[] = { 0xDE, 0xAD, 0xBE, 0xEF, 0xFE, 0xED };
IPAddress arduinoIp(192, 168, 1, 50);
IPAddress dnsServer(192, 168, 1, 1);
IPAddress gateway(192, 168, 1, 1);
IPAddress subnet(255, 255, 255, 0);
IPAddress broker(192, 168, 1, 152);
const uint16_t BROKER_PORT = 1883;
const char* CLIENT_ID = "deck-lights";
const char* TOPIC_CMD = "home/lights/commands";
const char* TOPIC_NOTIFY = "home/lights/notifications";

CRGB leds[NUM_LEDS];
EthernetClient ethClient;
PubSubClient mqtt(ethClient);

void publishStatus(const char* msg) {
  mqtt.publish(TOPIC_NOTIFY, msg);
  Serial.println(msg);
}

void mqttCallback(char* topic, byte* payload, unsigned int length) {
  char buf[32];
  if (length >= sizeof(buf)) {
    publishStatus("ERR payload too long");
    return;
  }
  memcpy(buf, payload, length);
  buf[length] = '\0';

  int idx, r, g, b;
  if (sscanf(buf, "%d %d %d %d", &idx, &r, &g, &b) != 4) {
    publishStatus("ERR bad format, expected: <idx> <r> <g> <b>");
    return;
  }
  if (idx < 0 || idx >= NUM_LEDS) {
    publishStatus("ERR index out of range");
    return;
  }
  if (r < 0 || r > 255 || g < 0 || g > 255 || b < 0 || b > 255) {
    publishStatus("ERR rgb out of range");
    return;
  }

  leds[idx] = CRGB(r, g, b);
  FastLED.show();

  char ack[32];
  snprintf(ack, sizeof(ack), "OK %d %d %d %d", idx, r, g, b);
  publishStatus(ack);
}

void reconnect() {
  while (!mqtt.connected()) {
    Serial.print(F("MQTT connecting... "));
    if (mqtt.connect(CLIENT_ID)) {
      Serial.println(F("connected"));
      mqtt.subscribe(TOPIC_CMD);
      publishStatus("online");
    } else {
      Serial.print(F("rc="));
      Serial.print(mqtt.state());
      Serial.println(F(", retry in 2s"));
      delay(2000);
    }
  }
}

void setup() {
  Serial.begin(9600);
  while (!Serial) {}
  delay(1000);

  pinMode(SD_CS_PIN, OUTPUT);
  digitalWrite(SD_CS_PIN, HIGH);
  pinMode(W5100_CS_PIN, OUTPUT);

  FastLED.addLeds<NEOPIXEL, DATA_PIN>(leds, NUM_LEDS);
  FastLED.clear();
  FastLED.show();

  Serial.println(F("=== Ethernet hardware check ==="));
  Ethernet.init(W5100_CS_PIN);

  EthernetHardwareStatus hw = Ethernet.hardwareStatus();
  Serial.print(F("Hardware: "));
  switch (hw) {
    case EthernetNoHardware: Serial.println(F("NONE (detection failed; some W5100 clones report this even when working)")); break;
    case EthernetW5100:      Serial.println(F("W5100")); break;
    case EthernetW5200:      Serial.println(F("W5200")); break;
    case EthernetW5500:      Serial.println(F("W5500")); break;
    default:                 Serial.println(F("unknown")); break;
  }

  Serial.println(F("Configuring static IP..."));
  Ethernet.begin(mac, arduinoIp, dnsServer, gateway, subnet);

  Serial.print(F("Link: "));
  EthernetLinkStatus link = Ethernet.linkStatus();
  Serial.println(link == LinkON ? F("ON") : (link == LinkOFF ? F("OFF") : F("UNKNOWN")));

  Serial.print(F("IP: "));
  Serial.println(Ethernet.localIP());

  if (Ethernet.localIP() == IPAddress(0, 0, 0, 0)) {
    Serial.println(F("WARN: IP is 0.0.0.0; SPI to W5100 likely not working."));
    Serial.println(F("      Re-seat shield, check pins 10-13. Will keep trying MQTT anyway."));
  }

  mqtt.setServer(broker, BROKER_PORT);
  mqtt.setCallback(mqttCallback);
}

void loop() {
  if (!mqtt.connected()) reconnect();
  mqtt.loop();
}
