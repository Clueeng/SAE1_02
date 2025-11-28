import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class BrouilleImage {

    public static void main(String[] args) throws IOException {
        if(args.length < 2) {
            System.err.println("Usage: java Brouillimg <image_claire> <clé> [image_sortie]");
            System.exit(1);
        }

        String inPath = args[0];
        String outPath = (args.length >= 3) ? args[2] : "out.png";
        String outUnscramblePath = (args.length >= 3) ? "unscrambled_" + args[2] : "unscrambled_out.png";
        // Masque 0x7FFF pour garantir que la clé ne dépasse pas les 15 bits
        int key = Integer.parseInt(args[1]) & 0x7FFF ;

        BufferedImage inputImage = ImageIO.read(new File(inPath));
        if (inputImage == null) {
            throw new IOException("Format d’image non reconnu: " + inPath);
        }

        final int height = inputImage.getHeight();
        final int width = inputImage.getWidth();
        System.out.println("Dimensions de l'image : " + width + "x" + height);
        // Pré‑calcul des lignes en niveaux de gris pour accélérer le calcul du critère
        int[][] inputImageGL = rgb2gl(inputImage);
        int[] perm = generatePermutation(height, key);

        BufferedImage scrambledImage = scrambleLines(inputImage, perm);
        ImageIO.write(scrambledImage, "png", new File(outPath));
        System.out.println("Image écrite: " + outPath);
        BufferedImage unscrambledImage = unscrambleLines(scrambledImage, perm);
        ImageIO.write(unscrambledImage, "png", new File(outUnscramblePath));
        System.out.println("Image ddecrypte");


        int broken = breakKey(scrambledImage);
        System.out.printf("Clef trouvee: %s\n", broken);
        int[] brokenPerm = generatePermutation(height, broken);
        BufferedImage imageCassee = unscrambleLines(scrambledImage, brokenPerm);
        ImageIO.write(imageCassee, "png", new File("broken.png"));
    }


    /**
     * Convertit une image RGB en niveaux de gris (GL).
     * @param inputRGB image d'entrée en RGB
     * @return tableau 2D des niveaux de gris (0-255)
     */
    public static int[][] rgb2gl(BufferedImage inputRGB) {
        final int height = inputRGB.getHeight();
        final int width = inputRGB.getWidth();
        int[][] outGL = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = inputRGB.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                // luminance simple (évite float)
                int gray = (r * 299 + g * 587 + b * 114) / 1000;
                outGL[y][x] = gray;
            }
        }
        return outGL;
    }

    /**
     * Génère une permutation des entiers 0..size-1 en fonction d'une clé.
     * @param size taille de la permutation
     * @param key clé de génération (15 bits)
     * @return tableau de taille 'size' contenant une permutation des entiers 0..size-1
     */

    /*
    Une ligne d’indice idLigne (la ligne 0 étant tout en haut) dans l’image d’entrée
    en clair sera ainsi déplacée en position ((r + (2s+1)idLigne)%size) où
    r est un décalage (offset) codé sur 8 bits
    s est un pas (step) codé sur 7 bits
    l’ensemble (r,s) forme la clé de chiffrement
     */
    public static int[] generatePermutation(int size, int key){
        int[] scrambleTable = new int[size];
        // scrambleTable[i] = ligne i pour l'instant
        // le but: generer la permutation
        for (int i = 0; i < size; i++) {
            scrambleTable[i] = scrambledId(i, size, key);
        }
        return scrambleTable;
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
//        System.out.println("Debug: " + n + " devient " + Integer.toBinaryString(n));
//        System.out.println("Debug: " + n + " devient avec [" + lower + ", " + upper + "]:\t " + stringBuilder.toString());
        return stringBuilder.toString();
    }


    /**
     * Mélange les lignes d'une image selon une permutation donnée.
     * @param inputImg image d'entrée
     * @param perm permutation des lignes (taille = hauteur de l'image)
     * @return image de sortie avec les lignes mélangées
     */
    public static BufferedImage scrambleLines(BufferedImage inputImg, int[] perm){
        int width = inputImg.getWidth();
        int height = inputImg.getHeight();
        if (perm.length != height) throw new IllegalArgumentException("Taille d'image <> taille permutation");

        BufferedImage toScramble = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for(int idLigne = 0; idLigne < height; idLigne++) {
            for(int pixelLigne = 0; pixelLigne < width; pixelLigne++) {
                // on change une ligne pixel par pixel
                // on change cette ligne par la ligne ((r + (2s+1)idLigne)%size)
                int lignePermut = perm[idLigne];
                toScramble.setRGB(pixelLigne, idLigne, inputImg.getRGB(pixelLigne, lignePermut));
            }
        }
        return toScramble;
    }

    public static BufferedImage unscrambleLines(BufferedImage scrambledImage, int[] perm) {
        int width = scrambledImage.getWidth();
        int height = scrambledImage.getHeight();
        if(perm.length != height) throw new IllegalArgumentException("Taille d'image <> taille permutation");

        BufferedImage unscrambled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for(int idLigne = 0; idLigne < height; idLigne++) {
            for(int pixelLigne = 0; pixelLigne < width; pixelLigne++) {
                int lignePermut = perm[idLigne];
                unscrambled.setRGB(pixelLigne, lignePermut, scrambledImage.getRGB(pixelLigne, idLigne));
            }
        }
        return unscrambled;
    }

    /**
     * Renvoie la position de la ligne id dans l'image brouillée.
     * @param id  indice de la ligne dans l'image claire (0..size-1)
     * @param size nombre total de lignes dans l'image
     * @param key clé de brouillage (15 bits)
     * @return indice de la ligne dans l'image brouillée (0..size-1)
     */
    public static int scrambledId(int id, int size, int key) {
        String rBits = getBits(key, 7, 14);
        String sBits = getBits(key, 0, 6); // 10
        int r = Integer.parseInt(rBits, 2); // 8 premiers bits de key
        int s = Integer.parseInt(sBits, 2); // 7 derniers bits de key
//        System.out.println("Clef de " + Integer.toBinaryString(key).length() + " bits");
//        System.out.println("decKey=" + key);
//        System.out.println("binKey=" + Integer.toBinaryString(key));
//        System.out.println("rBits="+rBits);
//        System.out.println("sBits="+sBits);
//        System.out.println("r="+r);
//        System.out.println("s="+s);
        return ((r + ((2 * s) + 1) * id) % size);
    }

    /**
     * Retourne la distance euclidienne de deux lignes differentes
     * @param line1 premiere ligne de pixel a comparer
     * @param line2 seconde ligne de pixel a comparer
     * @param n la longueur de l'image (width)
     * @return la distance euclidenne des deux lignes
     */
    public static double euclideanDistance(int[] line1, int[] line2, int n) {
        double total = 0.0f;
        for(int i = 0; i <= n - 1; i++) {
            // calcul en utilisant la formule sur p
            total += (line1[i] - line2[i]) * (line1[i] - line2[i]);
        }
        return Math.sqrt(total);
    }

    // https://stackoverflow.com/questions/17183587/convert-integer-color-value-to-rgb
    static int[] getRgbLigne(int ligne, int width, BufferedImage image) {
        int[] pixelPremier = new int[width];
        for(int pixelLigne = 0; pixelLigne < width; pixelLigne++) {
            int col = image.getRGB(pixelLigne, ligne);
            int red =  (col >> 16) & 0xFF;
            int green = (col >> 8) & 0xFF;
            int blue = (col) & 0xFF;
            pixelPremier[pixelLigne] = red + green + blue;
        }
        return pixelPremier;
    }

    public static double scoreEuclidean(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double score = 0.0f;
        for (int ligne = 0; ligne < height-1; ligne+=2) {
            int[] pixelPremier = getRgbLigne(ligne, width, image);
            int[] pixelDeux = getRgbLigne(ligne + 1, width, image);

            score += euclideanDistance(pixelPremier, pixelDeux, width);
        }
        return (int) score;
    }

    public static double pearsonScore(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        double score = 0.0f;

        for (int ligne = 0; ligne < height-1; ligne+=2) {
            int[] pixelPremier = getRgbLigne(ligne, width, image);
            int[] pixelDeux = getRgbLigne(ligne + 1, width, image);
            score += pearsonLines(image, pixelPremier, pixelDeux);
        }
        return score / height;
    }

    public static double pearsonLines(BufferedImage image, int[] line1, int[] line2) {
        // p(x, y)
        int width = image.getWidth();

        double avgLine1 = 0;
        double avgLine2 = 0;

        for(int line = 0; line < line1.length; line++)
            avgLine1 += line1[line];
        avgLine1 = (avgLine1 + 1) / line1.length;

        for(int line = 0; line < line2.length; line++)
            avgLine2 += line2[line];
        avgLine2 = (avgLine2 + 1) / line2.length;

        double sumTop = 0.0;
        for(int i = 1; i < width; i++) {
            sumTop += (line1[i] - avgLine1) * (line2[i] - avgLine2);
        }

        double sumLeft = 0.0;
        for(int i = 1; i < width; i++) {
            sumLeft += (line1[i] - avgLine1) * (line1[i - 1] - avgLine1);
        }

        double sumRight = 0.0;
        for(int i = 1; i < width; i++) {
            sumRight += (line2[i] - avgLine2) *  (line2[i] - avgLine2);
        }

        return (sumTop / (Math.sqrt(sumLeft) * Math.sqrt(sumRight)));
    }

    static int indexMax(double[] array) {
        int maxIndex = 0;
        for(int i = 1; i < array.length; i++) {
            if(array[i] > array[maxIndex]){
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public static int breakKey(BufferedImage scrambled) {
        int maxKeys = (int) Math.pow(2, 15);
        double[] results = new double[maxKeys];
        double oldMax = -1.0;


        long startTime = System.nanoTime();
        System.out.printf("\n");

        for(int key = 0; key < maxKeys; key++) {
            int height = scrambled.getHeight();
            int[] perm = generatePermutation(height, key);
            BufferedImage tested = unscrambleLines(scrambled, perm);
            // TODO : Fix pearson
            results[key] = scoreEuclidean(tested);
            if(results[key] > oldMax) {
                oldMax = results[key];
//                System.out.printf("%f, %s/%s\n", results[key], key, maxKeys);
            }
            // on prend le temps total qui s'est passe, ensuite on le divise par le nombre de clef deja check
            // ca donne le temps moyen d'une clef
            int keysDone = key + 1;
            int remainKey = maxKeys - keysDone;
            long duration = System.nanoTime() - startTime;
            long avgDuration = duration / keysDone;

            long estimatedTimeLeft = avgDuration * remainKey;

            // c'est des nano
            double converted = estimatedTimeLeft / 1e9f;

            String remainingTimeString = String.format("%.2f secondes", converted);

            // https://gist.github.com/fnky/458719343aabd01cfb17a3a4f7296797
            double perc = (double) keysDone / maxKeys * 100.0;
            System.out.printf("\n\u001B[2A\u001B[0JCassage: %d/%d (%.2f%%)\nTemps restant: %s",
                    keysDone, maxKeys, perc, remainingTimeString);
        }

        return indexMax(results);
    }

}