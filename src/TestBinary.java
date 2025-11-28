public class TestBinary {
    public static void main(String[] args) {
        int test = 256;
        String toBit = getBits(test, 1, 8);
        System.out.println(test);
        System.out.println(toBit);
    }
    // https://stackoverflow.com/questions/14145733/how-can-one-read-an-integer-bit-by-bit-in-java
    static String getBit(int n, int k) {
        return "" + ((n >> k) & 1);
    }
    static String getBits(int n, int lower, int upper) {
        StringBuilder stringBuilder = new StringBuilder();
        for(int i = upper; i >= lower; i--) {
            stringBuilder.append(getBit(n, i));
        }
        System.out.println("Debug: " + n + " devient avec [" + lower + ", " + upper + "]:\t " + stringBuilder.toString());
        return stringBuilder.toString();
    }
}
