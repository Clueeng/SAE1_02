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

    public static int[] generatePermutation(int size, int key){
        int[] scrambleTable = new int[size];
        for (int i = 0; i < size; i++) scrambleTable[i] = i;
        return scrambleTable;
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

//        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage out = inputImg;

        for(int i = 0; i < height - 3; i+=2) {
            for(int j = 0; j < width - 1; j++) {
                int oldRgb = out.getRGB(j, i);
                // on inverse les deux lignes
                int newRgb = out.getRGB(j, i);
                out.setRGB(j, i, newRgb);
                out.setRGB(j, i + 1, oldRgb);
            }
        }
        return out;
    }

    public static BufferedImage unscrambleLines(BufferedImage scrambledImage, int[] perm) {
        return null;
    }

    /**
     * Renvoie la position de la ligne id dans l'image brouillée.
     * @param id  indice de la ligne dans l'image claire (0..size-1)
     * @param size nombre total de lignes dans l'image
     * @param key clé de brouillage (15 bits)
     * @return indice de la ligne dans l'image brouillée (0..size-1)
     */
    public static int scrambledId(int id, int size, int key) {
        return id;
    }

}