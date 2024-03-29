
#include <SoftwareSerial.h>

SoftwareSerial BTSerial(10, 11); // RX | TX
int incomingByte = 0;

void setup()
{
  pinMode(2, OUTPUT);  

  digitalWrite(2, HIGH);
  
  Serial.begin(9600);
  Serial.println("Write input...");
  BTSerial.begin(9600); 
}

void loop()
{

  // Keep reading from HC-05 and send to Arduino Serial Monitor
  if (BTSerial.available())
  {
    incomingByte = BTSerial.read();
    Serial.write(incomingByte);

    if(incomingByte == 49) digitalWrite(2, HIGH);
    else digitalWrite(2, LOW);
  }

  // Keep reading from Arduino Serial Monitor and send to HC-05
  if (Serial.available())
  {
    incomingByte = Serial.read();
    BTSerial.write(incomingByte);
  }
}
