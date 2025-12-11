

/**
 *  - Device driver
 *
 * thx Robert Savage & martin86
 *
 *
 *  by Mephdog
 *
 *
 */

import android.os.Handler;
import android.util.Log;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.SpiDevice;

import java.io.IOException;

/**
 * Device driver for MCP23S17 chip on PiFace Digital 2
 */

public class PiFaceDigital2 implements AutoCloseable {


    public interface PiFaceDigital2Interface {

        /**
         * Inputs
         * @param value value information
         */

        void PiFaceDigital2Input0(String value);
        void PiFaceDigital2Input1(String value);
        void PiFaceDigital2Input2(String value);
        void PiFaceDigital2Input3(String value);
        void PiFaceDigital2Input4(String value);
        void PiFaceDigital2Input5(String value);
        void PiFaceDigital2Input6(String value);
        void PiFaceDigital2Input7(String value);

    }

    private PiFaceDigital2Interface mInterface;

    private static final String TAG          = PiFaceDigital2.class.getSimpleName();
    //private static final String GPIO_PORT    = "BCM25";
    private static final String GPIO_PORT    = "BCM25";
    private static final char[] hexArray     = "0123456789ABCDEF".toCharArray();

    private static final byte   IODIRA       = 0x00;
    private static final byte   IODIRB       = 0x01;
    private static final byte   IPOLA        = 0x02;
    private static final byte   IPOLB        = 0x03;
    private static final byte   GPINTENA     = 0x04;
    private static final byte   GPINTENB     = 0x05;
    private static final byte   DEFVALA      = 0x06;
    private static final byte   DEFVALB      = 0x07;
    private static final byte   INTCONA      = 0x08;
    private static final byte   INTCONB      = 0x09;
    private static final byte   IOCONA       = 0x0A;
    private static final byte   IOCONB       = 0x0B;
    private static final byte   GPPUA        = 0x0C;
    private static final byte   GPPUB        = 0x0D;
    private static final byte   INTFA        = 0x0E;
    private static final byte   INTFB        = 0x0F;
    private static final byte   INTCAPA      = 0x10;
    private static final byte   INTCAPB      = 0x11;
    private static final byte   GPIOA        = 0x12;
    private static final byte   GPIOB        = 0x13;
    private static final byte   OLATA        = 0x14;
    private static final byte   OLATB        = 0x15;

    private static final byte SPI_SLAVE_READ = 1;
    private static final byte SPI_SLAVE_WRITE = 0;
    private static final byte SPI_SLAVE_ID = 0x40;
    private static final byte SPI_SLAVE_ADDR = 0; // 0, 1, 2

    private static final byte   BaseAddressWrite = 0x40;
    private static final byte   BaseAddressRead  = 0x41;

    private static byte[] readBuffer3  = new byte[3];
    private static byte[] writeBuffer3 = new byte[3];

    private final Handler  mHandler;
    private final Runnable mRunnable;

    private final SpiDevice mSpiDevice;
    private final Gpio      mGpio;

    private GpioCallback mGpioCallback = new GpioCallback() {
        @Override
        public boolean onGpioEdge(Gpio gpio) {


            byte[] array = new byte[0];

            try {
                array = new byte[]{ readSpiDevice((byte) 0x13)};
            } catch (IOException e) {
                e.printStackTrace();
            }

            String currentButtonValue = PiFaceDigital2.bytesToHex(array);

            switch (currentButtonValue) {
                case "FF":
                    Log.i(TAG, "onGpioEdge: " + currentButtonValue);
                    mInterface.PiFaceDigital2Input0(currentButtonValue);
                    break;
                case "FE":
                    Log.i(TAG, "onGpioEdge: " + currentButtonValue);
                    mInterface.PiFaceDigital2Input1(currentButtonValue);
                    break;
                case "FD":
                    Log.i(TAG, "onGpioEdge: " + currentButtonValue);
                    mInterface.PiFaceDigital2Input2(currentButtonValue);
                    break;
                case "FB":
                    Log.i(TAG, "onGpioEdge: " + currentButtonValue);
                    mInterface.PiFaceDigital2Input3(currentButtonValue);
                    break;
                case "F7":
                    Log.i(TAG, "onGpioEdge: " + currentButtonValue);
                    mInterface.PiFaceDigital2Input4(currentButtonValue);
                    break;

                    //todo add all inputs

                default:
                    Log.i(TAG, "onGpioEdge: " + currentButtonValue);
                    break;
            }


            return true;
        }

        @Override
        public void onGpioError(Gpio gpio, int error) {
            Log.w(TAG, gpio + ": Error event " + error);
        }
    };




//    private GpioCallback mGpioCallback = new GpioCallback() {
//        @Override
//        public boolean onGpioEdge(Gpio gpio) {
//            Log.i(TAG, "Interrupt occurred");
//
//            try {
//                readSpiDevice(INTFB);
//                readSpiDevice(INTCAPB);
//
//                byte[] array = new byte[]{readSpiDevice(GPIOB)};
//
//                String currentButtonValue = bytesToHex(array);
//
//				/*switch (currentButtonValue) {
//					case "FF":
//						writeSpiDevice(GPIOA, (byte) 0xFF);
//						break;
//					case "FE":
//						writeSpiDevice(GPIOA, (byte) 0x03);
//						break;
//					case "FD":
//						writeSpiDevice(GPIOA, (byte) 0x0C);
//						break;
//					case "FB":
//						writeSpiDevice(GPIOA, (byte) 0x30);
//						break;
//					case "F7":
//						writeSpiDevice(GPIOA, (byte) 0xC0);
//						break;
//					default:
//						writeSpiDevice(GPIOA, (byte) 0x00);
//						break;
//				}*/
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//
//            return true;
//        }
//
//        @Override
//        public void onGpioError(Gpio gpio, int error) {
//            Log.w(TAG, gpio + ": Error event " + error);
//        }
//    };

    PiFaceDigital2(SpiDevice spiDevice, Gpio gpio, int spimode) throws IOException {
        mGpio = gpio; // This pin is for detecting changes on output ports

        mGpio.setDirection(Gpio.DIRECTION_IN);
        mGpio.setActiveType(Gpio.ACTIVE_LOW);
        mGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);

        mGpio.registerGpioCallback(mGpioCallback);

        mSpiDevice = spiDevice;
        mSpiDevice.setFrequency(10000000); // 10MHz
        mSpiDevice.setMode(spimode); // Mode 0 seems to work best for PiFaceDigital2
        mSpiDevice.setBitsPerWord(8);
        //mSpiDevice.setBitJustification(0);

        mHandler = new Handler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    readSpiDevice(GPIOB);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mHandler.postDelayed(mRunnable, 1000);
            }
        };

        startSpiDevice();
    }

//    public void registerCallback(GpioCallback GpioCallback) throws IOException {
//        try {
//            mGpio.registerGpioCallback(mGpioCallback);
//        } catch (IOException e) {
//            throw new IOException("Unable to register callback ", e);
//        }
//    }


    private void startSpiDevice() throws IOException {

        //        Write(IOCONA, 0x28); // BANK=0, SEQOP=1, HAEN=1 (Enable Addressing) interrupt not configured
        //io con modes...
//        private static final byte IOCON_UNUSED    = (byte)0x01;
//        private static final byte IOCON_INTPOL    = (byte)0x02;
//        private static final byte IOCON_ODR       = (byte)0x04;
//        private static final byte IOCON_HAEN      = (byte)0x08;
//        private static final byte IOCON_DISSLW    = (byte)0x10;
//        private static final byte IOCON_SEQOP     = (byte)0x20;
//        private static final byte IOCON_MIRROR    = (byte)0x40;
//        private static final byte IOCON_BANK_MODE = (byte)0x80;
        writeSpiDevice(IOCONA, (byte) 0x28); // 28, 18 or 08, not sure about the difference


        //        Write(IODIRA, 0x00); // GPIOA As Output
        //        Write(IODIRB, 0xFF); // GPIOB As Input
        writeSpiDevice(IODIRA, (byte) 0x00);
        writeSpiDevice(IODIRB, (byte) 0xFF);



        //writeSpiDevice(GPPUA, (byte) 0x00);

        //        Write(GPPUB, 0xFF); // configure pull ups
        writeSpiDevice(GPPUB, (byte) 0xFF);

        writeSpiDevice(IPOLA, (byte) 0xFF);
        writeSpiDevice(IPOLB, (byte) 0x00);

        //        Write(DEFVALB, 0x00); // normally high, only applicable if INTCONB == 0x0xFF
        writeSpiDevice(DEFVALB, (byte) 0x00);

        //        Write(INTCONB, 0x00); // interrupt occurs upon pin change
        writeSpiDevice(IOCONB, (byte) 0x00);

        //        Write(GPINTENB, 0xFF); // enable all interrupts
        writeSpiDevice(GPINTENB, (byte) 0xFF);

        //        Write(GPIOA, 0x55); // drive all outputs low
        writeSpiDevice(GPIOA, (byte) 0x00);

//        Write(IOCONA, 0x28); // BANK=0, SEQOP=1, HAEN=1 (Enable Addressing) interrupt not configured
//        Write(IODIRA, 0x00); // GPIOA As Output
//        Write(IODIRB, 0xFF); // GPIOB As Input
//        Write(GPPUB, 0xFF); // configure pull ups
//
//        Write(DEFVALB, 0x00); // normally high, only applicable if INTCONB == 0x0xFF
//        Write(INTCONB, 0x00); // interrupt occurs upon pin change
//        //Write(INTCONB, 0xFF); // interrupt occurs when pin values become DEFVALB values
//        Write(GPINTENB, 0xFF); // enable all interrupts
//        Write(GPIOA, 0x55); // drive all outputs low

        mRunnable.run();
    }

    private void writeSpiDevice(byte address, byte data) throws IOException {
        if (mSpiDevice != null) {
            writeBuffer3[0] = BaseAddressWrite;
            writeBuffer3[1] = address;
            writeBuffer3[2] = data;
            mSpiDevice.write(writeBuffer3, writeBuffer3.length);
        }
    }

    private void write(byte address, byte data) throws IOException {
        if (mSpiDevice != null) {
            byte[] buffer = new byte[3];
            buffer[0] = SPI_SLAVE_ID | ((SPI_SLAVE_ADDR << 1) & 0x0E) | SPI_SLAVE_WRITE;
            buffer[1] = address;
            buffer[2] = data;
            mSpiDevice.write(buffer, buffer.length);
        }
    }

    private byte readSpiDevice(byte address) throws IOException {
        if (mSpiDevice != null) {
            writeBuffer3[0] = BaseAddressRead;
            writeBuffer3[1] = address;
            writeBuffer3[2] = 0;
            mSpiDevice.transfer(writeBuffer3, readBuffer3, writeBuffer3.length);

            Log.d(TAG, "readSpiDevice - " + bytesToHex(readBuffer3));

            return readBuffer3[2];
        }
        return 0;
    }

    private byte read(byte address) throws IOException{
        if(mSpiDevice != null) {
            byte[] buffer = new byte[2];
            buffer[0] = SPI_SLAVE_ID | ((SPI_SLAVE_ADDR << 1) & 0x0E) | SPI_SLAVE_READ;
            buffer[1] = address;
            byte[] read_buffer = new byte[1];
            mSpiDevice.transfer(buffer, read_buffer, buffer.length);
            return read_buffer[0];
        }
        return 0;
    }

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * Create a new driver for PiFace Digital 2.
     *
     * @param spiBusPort Name of the SPI bus
     */
    public static PiFaceDigital2 create(String spiBusPort, int spimode) throws IOException {
        PeripheralManager peripheralManager = PeripheralManager.getInstance();

        try {
            return new PiFaceDigital2(peripheralManager.openSpiDevice(spiBusPort), peripheralManager.openGpio(GPIO_PORT), spimode);
        } catch (IOException e) {
            throw new IOException("Unable to open SPI device in bus port " + spiBusPort, e);
        }
    }


    /**
     * Turn on or off LED on specific position.
     *
     * @param position Position of the LED, value must be between 0 and 7
     * @param onOff    True to turn on, false to turn off
     */
    public void setLED(int position, boolean onOff) {
        setOutputPin(position, onOff);
    }

    //public void getInputPin

    /**
     * Set output pin on specific position on or off.
     *
     * @param position Position of the output pin, value must be between 0 and 7
     * @param onOff    True to turn on, false to turn off
     */
    public void setOutputPin(int position, boolean onOff) {
        if (position > 7 || position < 0) {
            Log.e(TAG, position + " is not a valid output pin / LED position");
        } else {
            try {
                byte LEDs = readSpiDevice(GPIOA);

                if (onOff) {
                    LEDs = (byte) (LEDs | (1 << position));
                } else {
                    LEDs = (byte) (LEDs & ~(1 << position));
                }

                writeSpiDevice(GPIOA, LEDs);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Turn on or off relay on specific position.
     *
     * @param position Position of the relay, value must be either 0 or 1
     * @param onOff    True to turn on, false to turn off
     */
    public void setRelay(int position, boolean onOff) {
        if (position > 1 || position < 0) {
            Log.e(TAG, position + " is not a valid relay position");
        } else {
            setOutputPin(position, onOff);
        }
    }

    /**
     * Releases the SPI interface.
     */
    @Override
    public void close() throws Exception {
        mGpio.unregisterGpioCallback(mGpioCallback);
        mGpio.close();
        mSpiDevice.close();
    }
}