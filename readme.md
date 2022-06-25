# NFC NFCA Tag Hex dump

This app read the complete content of a Tag of type NTAG21x (NTAG213, NTAG215 or NTAG216).

As the tag has 3 sections each will be read separately:

**a) Header section:**  stores the serial number of the tag, the static lock bytes and the 
Capability Container (CC) that is needed for NDEF-usage.

**b) User section:** this is the user memory of 144 bytes (NTAG213), 504 bytes (NTAG215) or 
888 bytes (NTAG216). Usually all data we write on the tag is saved in this section.

**c) Footer section:** here we find the dynamic lock bytes, the Configuration pages (0 and 1), 
the password and the PACK fields.

The app dumps the complete memory and tries to show the content in ASCII. As the password and 
PACK are not readable we just see 0x00 instead.

This app uses the low level protocol **NFCA** for the communication with the tag. There should be an 
automated font size adjusting on the dump data but I'm lazy at this point, just fit the value in the 
MainActivity-view to your needs:

```plaintext
<TextView
    android:id="@+id/tvMainReadResult"
    ...
    android:textSize="14sp"
    android:typeface="monospace"
    android:textStyle="normal" />
```
