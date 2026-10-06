# Wiring Notes

## Known Risks
- Breadboard connections are not soldered, vibration from motors 
  can loosen wires during gameplay
- 433MHz antenna not soldered onto transmitters, reduces range
- All components share breadboard power rails, a short circuit 
  on one component could affect all others
- MPU-9265 is on 3.3V while other components are on 5V, 
  accidentally swapping these will damage the accelerometer

## Unfinished Parts
- No PCB design exists, all wiring is on breadboards
- No strain relief on motor wires

## Power
- Cars powered by 6V battery (4xAA) connected to Nano VIN and L298N 12V