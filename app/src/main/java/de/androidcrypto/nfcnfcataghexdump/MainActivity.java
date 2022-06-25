package de.androidcrypto.nfcnfcataghexdump;

import android.content.Context;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.NfcA;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements NfcAdapter.ReaderCallback {

    TextView dumpField, readResult;
    private NfcAdapter mNfcAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dumpField = findViewById(R.id.tvMainDump1);
        readResult = findViewById(R.id.tvMainReadResult);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
    }

    // This method is run in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        System.out.println("NFC tag discovered");

        NfcA nfcA = null;

        try {
            nfcA = NfcA.get(tag);

            if (nfcA != null) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(),
                            "NFC tag is Nfca compatible",
                            Toast.LENGTH_SHORT).show();
                });

                // Make a Sound
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, 10));
                } else {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(200);
                }

                nfcA.connect();
                runOnUiThread(() -> {
                    readResult.setText("");
                });


                // check that the tag is a NTAG213/215/216 manufactured by NXP - stop if not
                String ntagVersion = NfcIdentifyNtag.checkNtagType(nfcA, tag.getId());
                if (ntagVersion.equals("0")) {
                    runOnUiThread(() -> {
                        readResult.setText("NFC tag is NOT of type NXP NTAG213/215/216");
                        Toast.makeText(getApplicationContext(),
                                "NFC tag is NOT of type NXP NTAG213/215/216",
                                Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                int nfcaMaxTranceiveLength = nfcA.getMaxTransceiveLength(); // important for the readFast command
                int ntagPages = NfcIdentifyNtag.getIdentifiedNtagPages();
                int ntagMemoryBytes = NfcIdentifyNtag.getIdentifiedNtagMemoryBytes();
                String tagIdString = getDec(tag.getId());
                String nfcaContent = "raw data of " + NfcIdentifyNtag.getIdentifiedNtagType() + "\n" +
                        "number of pages: " + ntagPages +
                        " total memory: " + ntagMemoryBytes +
                        " bytes\n" +
                        "tag ID: " + bytesToHex(NfcIdentifyNtag.getIdentifiedNtagId()) + "\n" +
                        "tag ID: " + tagIdString + "\n";
                nfcaContent = nfcaContent + "maxTranceiveLength: " + nfcaMaxTranceiveLength + " bytes\n";
                // read the complete memory depending on ntag type
                byte[] headerMemory = new byte[16]; // 4 pages of each 4 bytes, e.g. manufacturer data
                byte[] ntagMemory = new byte[ntagMemoryBytes]; // user memory, 888 byte for a NTAG216
                byte[] footerMemory = new byte[20]; // 5 pages, e.g. dyn. lock bytes, configuration pages, password & pack

                // read the content of the tag in several runs

                // first we are reading the header
                System.out.println("reading the header");
                headerMemory = getFastTagDataRange(nfcA, 0, 3);
                if (headerMemory == null) {
                    writeToUiAppend(readResult, "ERROR on reading header, aborted");
                }
                String dumpContentHeader = "Header content:\n" + HexDumpOwn.prettyPrint(headerMemory);

                int footerStart = 4 + ntagPages;
                int footerEnd = 4 + footerStart;
                System.out.println("reading the footer");
                footerMemory = getFastTagDataRange(nfcA, footerStart, footerEnd);
                if (footerMemory == null) {
                    writeToUiAppend(readResult, "ERROR on reading footer, aborted");
                }
                String dumpContentFooter = "Footer content:\n" + HexDumpOwn.prettyPrint(footerMemory);

                byte[] response;
                try {
                    //int nfcaMaxTranceiveLength = nfcA.getMaxTransceiveLength(); // my device: 253 bytes
                    int nfcaMaxTranceive4ByteTrunc = nfcaMaxTranceiveLength / 4; // 63
                    int nfcaMaxTranceive4ByteLength = nfcaMaxTranceive4ByteTrunc * 4; // 252 bytes
                    int nfcaNrOfFullReadings = ntagMemoryBytes / nfcaMaxTranceive4ByteLength; // 888 bytes / 252 bytes = 3 full readings
                    int nfcaTotalFullReadingBytes = nfcaNrOfFullReadings * nfcaMaxTranceive4ByteLength; // 3 * 252 = 756
                    int nfcaMaxTranceiveModuloLength = ntagMemoryBytes - nfcaTotalFullReadingBytes; // 888 bytes - 756 bytes = 132 bytes
                    nfcaContent = nfcaContent + "nfcaMaxTranceive4ByteTrunc: " + nfcaMaxTranceive4ByteTrunc + "\n";
                    nfcaContent = nfcaContent + "nfcaMaxTranceive4ByteLength: " + nfcaMaxTranceive4ByteLength + "\n";
                    nfcaContent = nfcaContent + "nfcaNrOfFullReadings: " + nfcaNrOfFullReadings + "\n";
                    nfcaContent = nfcaContent + "nfcaTotalFullReadingBytes: " + nfcaTotalFullReadingBytes + "\n";
                    nfcaContent = nfcaContent + "nfcaMaxTranceiveModuloLength: " + nfcaMaxTranceiveModuloLength + "\n";

                    for (int i = 0; i < nfcaNrOfFullReadings; i++) {
                        System.out.println("starting round: " + i);
                        response = getFastTagDataRange(nfcA, (4 + (nfcaMaxTranceive4ByteTrunc * i)), (4 + (nfcaMaxTranceive4ByteTrunc * (i + 1)) - 1));
                        if (response == null) {
                            writeToUiAppend(readResult, "ERROR on reading user memory, aborted");
                        } else {
                            // success: response contains ACK or actual data
                            System.arraycopy(response, 0, ntagMemory, (nfcaMaxTranceive4ByteLength * i), nfcaMaxTranceive4ByteLength);
                        }
                    } // for

                    // now we read the nfcaMaxTranceiveModuloLength bytes, for a NTAG216 = 132 bytes
                    response = getFastTagDataRange(nfcA, (4 + (nfcaMaxTranceive4ByteTrunc * nfcaNrOfFullReadings)), (4 + (nfcaMaxTranceive4ByteTrunc * nfcaNrOfFullReadings) + (nfcaMaxTranceiveModuloLength / 4)));
                    if (response == null) {
                        writeToUiAppend(readResult, "ERROR on reading user memory, aborted");
                    } else {
                        // success: response contains ACK or actual data
                        System.arraycopy(response, 0, ntagMemory, (nfcaMaxTranceive4ByteLength * nfcaNrOfFullReadings), nfcaMaxTranceiveModuloLength);
                    }
                    nfcaContent = nfcaContent + "fast reading complete: " + "\n" + bytesToHex(ntagMemory) + "\n";

                    String finalNfcaRawText = nfcaContent;
                    String dumpContent = dumpContentHeader + "\n\nUser memory content:\n" + HexDumpOwn.prettyPrint(ntagMemory);
                    dumpContent = dumpContent + "\n\n" + dumpContentFooter;
                    String finalDumpContent = dumpContent;
                    runOnUiThread(() -> {
                        dumpField.setText(finalDumpContent);
                        readResult.setText(finalNfcaRawText);
                        System.out.println(finalNfcaRawText);
                    });

                } finally {
                    try {
                        nfcA.close();
                    } catch (IOException e) {
                        writeToUiAppend(readResult, "ERROR IOException: " + e);
                    }
                }
            }
        } catch (IOException e) {
            writeToUiAppend(readResult, "ERROR IOException: " + e);
            e.printStackTrace();
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte b : bytes) result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    private String getDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result + "";
    }

    private void writeToUiAppend(TextView textView, String message) {
        runOnUiThread(() -> {
            String newString = message + "\n" + textView.getText().toString();
            textView.setText(newString);
        });
    }

    private void writeToUiToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
        });
    }

    private byte[] getFastTagDataRange(NfcA nfcA, int fromPage, int toPage) {
        byte[] response;
        byte[] command = new byte[]{
                (byte) 0x3A,  // FAST_READ
                (byte) (fromPage & 0x0ff),
                (byte) (toPage & 0x0ff),
        };
        try {
            response = nfcA.transceive(command); // response should be 16 bytes = 4 pages
            if (response == null) {
                // either communication to the tag was lost or a NACK was received
                writeToUiAppend(readResult, "ERROR on reading page");
                return null;
            } else if ((response.length == 1) && ((response[0] & 0x00A) != 0x00A)) {
                // NACK response according to Digital Protocol/T2TOP
                writeToUiAppend(readResult, "ERROR NACK received");
                // Log and return
                return null;
            } else {
                // success: response contains ACK or actual data
            }
        } catch (TagLostException e) {
            // Log and return
            writeToUiAppend(readResult, "ERROR Tag lost exception");
            return null;
        } catch (IOException e) {
            writeToUiAppend(readResult, "ERROR IOException: " + e);
            e.printStackTrace();
            return null;
        }
        return response;
    }

    private void showWirelessSettings() {
        Toast.makeText(this, "You need to enable NFC", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {

            if (!mNfcAdapter.isEnabled())
                showWirelessSettings();

            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag afer reading
            mNfcAdapter.enableReaderMode(this,
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this);
    }
}