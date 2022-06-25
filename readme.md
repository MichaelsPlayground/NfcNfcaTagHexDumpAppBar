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

