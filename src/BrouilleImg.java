import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * <h1>Groupe 7 (Leo Krakovinsky)</h1>
 * <p>Cette SAE a ete realisee par une seule personne</p>
 * <h2>Comment utiliser le programme</h2>
 * <h3> Brouiller une image </h3>
 * <p> java BrouilleImg [image] scramble [clef] </p>
 * <h3>Debrouiller une image</h3>
 * <p>java BrouilleImg [image] unscramble [clef] </p>
 * <h3>Casser une clef</h3>
 * java BrouilleImg [image] break
 */
public class BrouilleImg {
    public static void main(String[] args) throws IOException {
        if(args.length < 2) {
            System.err.println("Usage: java Brouillimg <image_claire> " +
                    "<mode> [image_sortie] [clef]");
            System.exit(1);
        }

        String inPath = args[0];
        String mode = args[1];

        BufferedImage inputImage = ImageIO.read(new File(inPath));
        if (inputImage == null) {
            throw new IOException("Format d’image non reconnu: " + inPath);
        }

        final int height = inputImage.getHeight();
        final int width = inputImage.getWidth();
        System.out.println("Dimensions de l'image : " + width + "x" + height);

        File outPutFolder=new File("./output/");
        if(!outPutFolder.exists() && !outPutFolder.mkdirs()) {
            System.err.println("Pas pu creer le dossier");
        }


        // atomic integer utilise
        AtomicInteger key = new AtomicInteger();
        switch (mode) {
            case "scramble" -> {
                key.set(Integer.parseInt(args[2]) & 0x7FFF);
                System.out.println("Brouillage de " + inPath + " avec la clef " + key + "...");
                System.out.println();
                int[] perm = generatePermutation(height, key.get());
                BufferedImage scrambledImage = scrambleLines(inputImage, perm);
                ImageIO.write(scrambledImage, "png",
                        new File("output/scrambled.png"));
                System.out.println("Fin, output: output/scrambled.png");
            }
            case "unscramble" -> {
                key.set(Integer.parseInt(args[2]) & 0x7FFF);
                System.out.println("Debrouillage de " + inPath + " avec la clef " + key + "...");
                int[] perm = generatePermutation(height, key.get());
                BufferedImage unscrambledImage =
                        unscrambleLines(inputImage, perm);
                ImageIO.write(unscrambledImage, "png",
                        new File("output/unscrambled.png"));
                System.out.println("Fin, output: output/unscrambled.png");
            }
            case "break" ->
                Profiler.profile(() -> {
                    String method = args[2];
                    System.out.println("Cassage de la clef de " + inPath + " avec l'algo " + method);
                    switch (method) {
                        case "pearson" ->
                                key.set(breakKeyPearson(inputImage));
                        case "euclidean" ->
                                key.set(breakKeyEuclidean(inputImage));
                        case "tv" ->
                                key.set(breakKeyTV(inputImage));
                        default -> {
                            System.out.println("Methode " +
                                    method + " mon existante");
                            System.exit(0);
                        }
                    }
                    System.out.println("Clef trouvee: " + key);
                    int[] perm = generatePermutation(height, key.get());
                    BufferedImage unscrambledImage =
                            unscrambleLines(inputImage, perm);
                    try {
                        ImageIO.write(unscrambledImage, "png",
                                new File("output/broken_" + method + ".png"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("Fin, output: output/broken_" + method + ".png");
                });
            default -> System.out.println("Mode non reconnu");
        }
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
     * @see #scrambledId(int, int, int)
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

    /**
     * Mélange les lignes d'une image selon une permutation donnée.
     * @param inputImg image d'entrée
     * @param perm permutation des lignes (taille = hauteur de l'image)
     * @return image de sortie avec les lignes mélangées
     */
    public static BufferedImage scrambleLines(BufferedImage inputImg,
                                              int[] perm){
        int width = inputImg.getWidth();
        int height = inputImg.getHeight();
        if (perm.length != height) throw new
            IllegalArgumentException("Taille d'image <> taille permutation");

        BufferedImage toScramble =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for(int idLigne = 0; idLigne < height; idLigne++) {
            int lignePermut = perm[idLigne];
            for(int pixelLigne = 0; pixelLigne < width; pixelLigne++) {
                // on change une ligne pixel par pixel
                // on change cette ligne par la ligne ((r + (2s+1)idLigne)%size)
                toScramble.setRGB(pixelLigne, lignePermut,
                        inputImg.getRGB(pixelLigne, idLigne));
            }
        }
        return toScramble;
    }

    public static BufferedImage unscrambleLines(BufferedImage scrambledImage
            , int[] perm) {
        int width  = scrambledImage.getWidth();
        int height = scrambledImage.getHeight();
        if(perm.length != height) throw new
                IllegalArgumentException("Taille d'image <> taille permutation");

        BufferedImage unscrambled =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] inverse = new int[height];
        for(int i = 0; i < height; i++) inverse[perm[i]] = i;

        for(int y = 0 ; y < height; y++) {
            int ySource = inverse[y];
            for(int x = 0; x < width; x++) {
                unscrambled.setRGB(x, ySource, scrambledImage.getRGB(x, y));
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
        int r = key & 0xFF;
        int s = (key >> 8) & 0x7F;
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

    public static double tvLine(int[] lineA, int[] lineB, int width) {
        double variation = 0.0;
        for (int i = 0; i < width; i++) {
            variation += Math.abs(lineA[i] - lineB[i]);
        }
        return variation;
    }

    /**
     * Donne un score sur deux lignes d'une image donnee en utilisant
     * l'algorithme de Pearson
     * @param line1 premiere ligne a comparer
     * @param line2 deuxieme ligne a comparer
     * @param width la longueur de l'image
     * @return le score des deux lignes
     */
    public static double pearsonLines(int[] line1, int[] line2, int width) {
        double avgLine1 = 0;
        double avgLine2 = 0;

        for(int line = 0; line < width; line++) {
            avgLine1 += line1[line];
            avgLine2 += line2[line];
        }
        avgLine1 = avgLine1 / width;
        avgLine2 = avgLine2 / width;

        double sumTop = 0.0;
        double sumVariance1 = 0.0;
        double sumVariance2 = 0.0;

        for(int i = 0; i < width; i++) {
            double diff1 = line1[i] - avgLine1;
            double diff2 = line2[i] - avgLine2;

            sumTop += diff1 * diff2;

            sumVariance1 += diff1 * diff1;
            sumVariance2 += diff2 * diff2;
        }

        if (sumVariance1 == 0.0 || sumVariance2 == 0.0) return 0.0;

        return (sumTop / (Math.sqrt(sumVariance1) * Math.sqrt(sumVariance2)));
    }

    /**
     * Casse la clef d'une image chiffree en utilisant l'algorithme d'Euclide
     * @param scrambled l'image cryptee
     * @return la clef d'origine
     * @see #generatePermutation(int, int)
     */
    public static int breakKeyEuclidean(BufferedImage scrambled) {
        int maxKeys = 1 << 15;
        int[][] scrambledGL = rgb2gl(scrambled);
        int height = scrambled.getHeight();
        int width = scrambledGL[0].length;

        // distances pre calculees
        double[][] dists = new double[height][height];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < height; j++) {
                dists[i][j] = euclideanDistance(scrambledGL[i], scrambledGL[j], width);
            }
        }

        double minScore = Double.POSITIVE_INFINITY;
        int bestKey = 0;

        for (int key = 0; key < maxKeys; key++) {
            int[] perm = generatePermutation(height, key);
            double currentScore = 0;
            for (int i = 0; i < height - 1; i++) {
                currentScore += dists[perm[i]][perm[i+1]];
            }
            if (currentScore < minScore) {
                minScore = currentScore;
                bestKey = key;
            }
        }
        return bestKey;
    }

    /**
     * Casse la clef d'une image chiffree en utilisant l'algorithme de la variation totale
     * @param scrambled image encryptee
     * @return la clef d'origine
     * @see #generatePermutation(int, int)
     */
    public static int breakKeyTV(BufferedImage scrambled) {
        int maxKeys = 1 << 15; // 32768
        int[][] scrambledGL = rgb2gl(scrambled);
        int height = scrambled.getHeight();

        // pre calcul des distances de lignes (optimisattion)
        double[][] lineDistances = new double[height][height];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < height; j++) {
                if (i == j) continue;
                lineDistances[i][j] = tvLine(scrambledGL[i], scrambledGL[j], scrambledGL[0].length);
            }
        }
        // valeur initiale
        double minTotalVariation = Double.POSITIVE_INFINITY;
        int bestKey = 0;

        for (int key = 0; key < maxKeys; key++) {
            int[] perm = generatePermutation(height, key);
            double currentTV = 0;

            for (int i = 0; i < height - 1; i++) {
                currentTV += lineDistances[perm[i]][perm[i+1]];
            }
            // compairaison
            if (currentTV < minTotalVariation) {
                minTotalVariation = currentTV;
                bestKey = key;
            }
        }
        return bestKey;
    }

    /**
     * Casse la clef d'une image chiffree en utilisant l'algorithme de Pearson
     * @param scrambled image encryptee
     * @return la clef d'origine
     * @see #generatePermutation(int, int) 
     */
    public static int breakKeyPearson(BufferedImage scrambled) {
        int maxKeys = 1 << 15;
        int[][] scrambledGL = rgb2gl(scrambled);
        int height = scrambled.getHeight();
        int width = scrambledGL[0].length;

        // distances pre calculees
        double[][] dists = new double[height][height];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < height; j++) {
                dists[i][j] = pearsonLines(scrambledGL[i], scrambledGL[j], width);
            }
        }

        double maxScore = Double.NEGATIVE_INFINITY;
        int bestKey = 0;

        for (int key = 0; key < maxKeys; key++) {
            int[] perm = generatePermutation(height, key);
            double currentScore = 0;
            for (int i = 0; i < height - 1; i++) {
                currentScore += dists[perm[i]][perm[i+1]];
            }
            if (currentScore >= maxScore) {
                maxScore = currentScore;
                bestKey = key;
            }
        }
        return bestKey;
    }

}