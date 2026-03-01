# Connecting ChronoGPS to the Bluetooth COM Port

## Step 1: Find the COM Port Number on Windows

1. Open **Device Manager** (right-click Start > Device Manager)
2. Expand **Ports (COM & LPT)**
3. Look for something like **"Standard Serial over Bluetooth link (COM5)"** — there may be two entries; you want the **outgoing** one
4. Note the COM port number (e.g., COM5)

**Alternative**: Settings > Bluetooth & devices > Devices > More Bluetooth settings > COM Ports tab. Look for the **Outgoing** port associated with your phone's name.

## Step 2: (Optional) Verify Data with PuTTY First

Before pointing ChronoGPS at it, you can confirm NMEA is flowing:

1. Open **PuTTY** (or any serial terminal)
2. Select **Serial**, enter the COM port (e.g., COM5), speed **9600**
3. Click Open
4. You should see NMEA sentences scrolling:
   ```
   $GPRMC,123456.00,A,4807.038,N,01131.000,E,...*XX
   $GPGGA,123456.00,4807.038,N,01131.000,E,...*XX
   ```
5. If data flows, close PuTTY (important — only one app can use the COM port at a time)

## Step 3: Configure ChronoGPS

1. Open **Decodium 3.0** (ChronoGPS is bundled with it)
2. Launch **ChronoGPS** from within Decodium
3. Select the COM port from the dropdown (e.g., COM5)
4. Set baud rate to **9600** (should be the default)
5. Click **Connect** / **Start**
6. You should see:
   - NMEA sentences appearing in the log
   - Time extracted from RMC sentences
   - Clock offset displayed (target: within ±50ms)

## Troubleshooting

- **No COM port listed**: Make sure the phone is paired AND the NMEA Bridge app shows "Connected" (the PC must have initiated the connection by opening the COM port)
- **COM port won't open**: Close any other app using that port (PuTTY, HyperTerminal, etc.)
- **Two COM ports listed**: Try the **Outgoing** one first; if that doesn't work, try the other
- **Data flows but no time sync**: Make sure the phone has a GPS fix (check the app — it should show "GPS: 3D Fix" and satellite count > 0). RMC sentences need status='A' (active fix) for ChronoGPS to accept the time
