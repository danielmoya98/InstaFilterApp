package com.example.proyectopdi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.camera.core.ImageProxy
import com.example.instafilterapp.R
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfKeyPoint
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.features2d.Feature2D
import org.opencv.features2d.Features2d
import org.opencv.features2d.SIFT
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import org.opencv.objdetect.Objdetect
import java.util.Collections
import java.util.Random
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class OpenUtils {


    // Función para detectar bordes en una imagen utilizando el algoritmo de Canny
    fun detectEdges(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat) // Convierte el bitmap a Mat para procesarlo con OpenCV

        val edges = Mat() // Matriz para almacenar los bordes detectados
        Imgproc.Canny(mat, edges, 80.0, 200.0) // Aplica el algoritmo de Canny para detección de bordes

        val lines = Mat() // Matriz para almacenar las líneas detectadas
        val p1 = Point()
        val p2 = Point()
        var a: Double
        var b: Double
        var x0: Double
        var y0: Double

        // Detecta líneas en la imagen usando la transformada de Hough
        Imgproc.HoughLines(edges, lines, 1.0, Math.PI/180.0, 140)
        for (i in 0 until lines.rows()) {
            val vec: DoubleArray = lines.get(i, 0)
            val rho: Double = vec[0]
            val theta: Double = vec[1]
            a = cos(theta)
            b = sin(theta)
            x0 = a * rho
            y0 = b * rho

            p1.x = Math.round(x0 + 1000 * (-b)).toDouble()
            p1.y = Math.round(y0 + 1000 * a).toDouble()

            p2.x = Math.round(x0 - 1000 * (-b)).toDouble()
            p2.y = Math.round(y0 - 1000 * a).toDouble()

            Imgproc.line(mat, p1, p2, Scalar(255.0, 255.0, 255.0), 1, Imgproc.LINE_AA, 0) // Dibuja las líneas detectadas
        }

        Utils.matToBitmap(mat, bitmap) // Convierte Mat de vuelta a Bitmap
        return bitmap
    }

    // Función para aplicar un umbral variable a la imagen
    fun variableThreshold(bitmap: Bitmap): Bitmap {
        val blockSize = 10 // Tamaño de los bloques para calcular el umbral local
        val c = 10 // Constante para ajustar el umbral calculado

        val width = bitmap.width
        val height = bitmap.height

        val thresholdBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888) // Bitmap para el resultado

        val pixels = IntArray(width * height) // Almacena los píxeles de la imagen
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height) // Extrae los píxeles del bitmap original

        // Itera sobre la imagen en bloques
        for (y in 0 until height step blockSize) {
            for (x in 0 until width step blockSize) {
                var blockSum = 0
                var pixelCount = 0

                // Calcula la suma de valores de gris en el bloque
                for (j in y until minOf(y + blockSize, height)) {
                    for (i in x until minOf(x + blockSize, width)) {
                        val pixel = pixels[j * width + i]
                        val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                        blockSum += gray
                        pixelCount++
                    }
                }

                val blockMean = blockSum.toDouble() / pixelCount
                val threshold = blockMean - c // Calcula el umbral para el bloque

                // Aplica el umbral al bloque
                for (j in y until minOf(y + blockSize, height)) {
                    for (i in x until minOf(x + blockSize, width)) {
                        val pixel = pixels[j * width + i]
                        val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                        pixels[j * width + i] = if (gray > threshold) Color.WHITE else Color.BLACK
                    }
                }
            }
        }

        thresholdBitmap.setPixels(pixels, 0, width, 0, 0, width, height) // Actualiza el bitmap de resultado con los píxeles procesados
        return thresholdBitmap
    }

    // Función para detectar rostros en una imagen y aplicar un desenfoque
    fun detectFace(bitmap: Bitmap, cascadeClassifier: CascadeClassifier): Bitmap {
        var mat = Mat()
        Utils.bitmapToMat(bitmap, mat) // Convierte Bitmap a Mat
        mat = CascadeRec(mat, cascadeClassifier) // Llama a la función auxiliar para realizar la detección y el desenfoque

        Utils.matToBitmap(mat, bitmap) // Convierte Mat de vuelta a Bitmap
        return bitmap
    }

    // Función auxiliar para detectar rostros y aplicar desenfoque
    private fun CascadeRec(mat: Mat, cascadeClassifier: CascadeClassifier): Mat {
        val mRgb = Mat()
        Imgproc.cvtColor(mat, mRgb, Imgproc.COLOR_RGBA2RGB) // Convierte de RGBA a RGB

        val height = mRgb.height()
        var absoluteFaceSize: Double = height * 0.1 // Calcula el tamaño mínimo del rostro

        var faces: MatOfRect = MatOfRect()
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(mRgb, faces, 1.1, 2, 2, Size(absoluteFaceSize, absoluteFaceSize), Size()) // Detecta rostros
        }

        val facesArray: Array<Rect> = faces.toArray() // Convierte resultados a un array

        for (i in facesArray.indices) {
            var submat: Mat = mat.submat(facesArray[i]) // Obtiene la región del rostro
            Imgproc.blur(submat, submat, Size(50.0, 50.0)) // Aplica un desenfoque al rostro
        }

        return mat // Devuelve el Mat procesado
    }

    // Función para aplicar el filtro Canny, que detecta bordes en una imagen.
    fun cannyFiltro(bitmap: Bitmap): Bitmap {
        val mat = Mat() // Crea una matriz OpenCV
        Utils.bitmapToMat(bitmap, mat) // Convierte el bitmap de Android en una matriz de OpenCV

        val edges = Mat() // Matriz para almacenar los bordes detectados

        // Aplica el algoritmo de Canny para la detección de bordes
        Imgproc.Canny(mat, edges, 80.0, 200.0) // Los umbrales para la detección pueden ajustarse según la necesidad

        // Convierte la matriz de bordes detectados de vuelta a bitmap
        Utils.matToBitmap(edges, bitmap)

        return bitmap // Devuelve el bitmap con los bordes detectados
    }

    // Función para detectar rostros y ojos en una imagen.
    fun detectFaceEye(bitmap: Bitmap, cascadeClassifier: CascadeClassifier, cascadeClassifier_eye: CascadeClassifier): Bitmap {
        var mat = Mat()
        Utils.bitmapToMat(bitmap, mat) // Convierte el bitmap en una matriz OpenCV
        mat = CascadeRec2(mat, cascadeClassifier, cascadeClassifier_eye) // Llama a la función auxiliar para procesar la detección

        // Convierte la matriz procesada de vuelta a bitmap
        Utils.matToBitmap(mat, bitmap)
        return bitmap // Devuelve el bitmap procesado
    }

    // Función auxiliar para detectar rostros y ojos utilizando clasificadores en cascada.
    private fun CascadeRec2(mat: Mat, cascadeClassifier: CascadeClassifier, cascadeClassifier_eye: CascadeClassifier): Mat {
        val mRgb = Mat()

        // Convierte la imagen de RGBA a RGB para el procesamiento con OpenCV
        Imgproc.cvtColor(mat, mRgb, Imgproc.COLOR_RGBA2RGB)

        val height = mRgb.height()
        var absoluteFaceSize: Double = height * 0.1 // Define el tamaño mínimo del rostro a detectar

        var faces: MatOfRect = MatOfRect()
        if (cascadeClassifier != null) {
            // Detecta rostros en la imagen
            cascadeClassifier.detectMultiScale(mRgb, faces, 1.1, 2, 2, Size(absoluteFaceSize, absoluteFaceSize), Size())
        }

        val facesArray: Array<Rect> = faces.toArray() // Convierte los resultados en un array

        // Procesa cada rostro detectado
        for (i in facesArray.indices) {
            // Dibuja un rectángulo alrededor de cada rostro detectado
            Imgproc.rectangle(mat, facesArray[i].tl(), facesArray[i].br(), Scalar(0.0, 255.0, 0.0, 255.0), 2)

            // Define la región de interés (ROI) para los ojos dentro del rostro
            var roi: Rect = Rect(facesArray[i].tl().x.toInt(), facesArray[i].tl().y.toInt(), facesArray[i].br().x.toInt() - facesArray[i].tl().x.toInt(),
                facesArray[i].br().y.toInt() - facesArray[i].tl().y.toInt())

            var cropped: Mat = Mat(mat, roi) // Extrae la ROI de la imagen
            val eyes: MatOfRect = MatOfRect()
            if (cascadeClassifier_eye != null) {
                // Detecta ojos dentro del ROI
                cascadeClassifier_eye.detectMultiScale(cropped, eyes, 1.15, 2,
                    Objdetect.CASCADE_FIND_BIGGEST_OBJECT or Objdetect.CASCADE_SCALE_IMAGE, Size(35.0, 35.0), Size())

                val eyesArray: Array<Rect> = eyes.toArray() // Convierte los resultados en un array

                // Dibuja un rectángulo alrededor de cada ojo detectado
                for (j in eyesArray.indices) {
                    var x1: Double = eyesArray[j].tl().x + facesArray[i].tl().x
                    var y1: Double = eyesArray[j].tl().y + facesArray[i].tl().y

                    var w1: Double = eyesArray[j].br().x - eyesArray[j].tl().x
                    var h1: Double = eyesArray[j].br().y - eyesArray[j].tl().y

                    var x2: Double = w1 + x1
                    var y2: Double = h1 + y1

                    Imgproc.rectangle(mat, Point(x1, y1), Point(x2, y2), Scalar(0.0, 255.0, 0.0, 255.0), 2)
                }
            }
        }

        return mat // Devuelve la matriz procesada con rostros y ojos marcados
    }






    // Función para aplicar un efecto de pixelización a una imagen
    fun applyPixelize(bitmap: Bitmap): Bitmap {
        val mat = Mat() // Crea una matriz OpenCV
        Utils.bitmapToMat(bitmap, mat) // Convierte el bitmap en una matriz OpenCV

        val mIntermediateMat = Mat() // Crea una matriz intermedia

        // Reduce el tamaño de la imagen para pixelizarla
        Imgproc.resize(mat, mIntermediateMat, mIntermediateMat.size(), 0.1, 0.1, Imgproc.INTER_NEAREST)
        // Vuelve a escalar la imagen al tamaño original para completar el efecto de pixelización
        Imgproc.resize(mIntermediateMat, mat, mat.size(), 0.0, 0.0, Imgproc.INTER_NEAREST)

        val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, resultBitmap) // Convierte la matriz procesada de vuelta a un bitmap

        mat.release() // Libera la matriz original

        return resultBitmap // Devuelve el bitmap pixelizado
    }

    // Función para aplicar un efecto de posterización a una imagen
    fun applyPosterize(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat) // Convierte el bitmap en una matriz OpenCV

        val mIntermediateMat = Mat() // Crea una matriz intermedia
        // Aplica el algoritmo de Canny para detectar bordes
        Imgproc.Canny(mat, mIntermediateMat, 80.0, 90.0)
        // Establece a negro los píxeles que no son bordes
        mat.setTo(Scalar(0.0, 0.0, 0.0, 255.0), mIntermediateMat)
        // Reduce la escala de colores para crear el efecto de posterización
        Core.convertScaleAbs(mat, mIntermediateMat, 1.0 / 16.0, 0.0)
        Core.convertScaleAbs(mIntermediateMat, mat, 16.0, 0.0)

        mIntermediateMat.release() // Libera la matriz intermedia

        Utils.matToBitmap(mat, bitmap) // Convierte la matriz procesada de vuelta a un bitmap

        return bitmap // Devuelve el bitmap posterizado
    }

    // Función para aplicar un efecto de zoom a una imagen
    fun applyZoom(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat) // Convierte el bitmap en una matriz OpenCV

        val sizeMat = mat.size()
        val rows = sizeMat.height.toInt()
        val cols = sizeMat.width.toInt()

        // Define la región que se ampliará
        val zoomCorner = mat.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10)
        // Define la ventana de zoom
        val mZoomWindow = mat.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100)

        // Realiza el efecto de zoom escalando la ventana de zoom
        Imgproc.resize(mZoomWindow, zoomCorner, zoomCorner.size(), 0.0, 0.0, Imgproc.INTER_LINEAR_EXACT)

        val wsize = mZoomWindow.size()
        // Dibuja un rectángulo alrededor de la ventana de zoom para destacarla
        Imgproc.rectangle(mZoomWindow, Point(1.0, 1.0), Point(wsize.width - 2.0, wsize.height - 2.0), Scalar(255.0, 0.0, 0.0, 255.0), 2)

        zoomCorner.release() // Libera la región ampliada
        mZoomWindow.release() // Libera la ventana de zoom

        Utils.matToBitmap(mat, bitmap) // Convierte la matriz procesada de vuelta a un bitmap
        return bitmap // Devuelve el bitmap con el efecto de zoom aplicado
    }



    fun applySepia(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Define el kernel de sepia
        val mSepiaKernel = Mat(4, 4, CvType.CV_32F)
        mSepiaKernel.put(0, 0, 0.189, 0.769, 0.393, 0.0)  // R
        mSepiaKernel.put(1, 0, 0.168, 0.686, 0.349, 0.0)  // G
        mSepiaKernel.put(2, 0, 0.131, 0.534, 0.272, 0.0)  // B
        mSepiaKernel.put(3, 0, 0.0, 0.0, 0.0, 1.0)         // A

        // Aplica el efecto sepia a toda la imagen
        Core.transform(mat, mat, mSepiaKernel)

        Utils.matToBitmap(mat, bitmap)

        return bitmap
    }


    fun applySobel(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        // Convertir a escala de grises
        val gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)

        // Aplicar efecto Sobel a toda la imagen en escala de grises
        val mIntermediateMat = Mat()
        Imgproc.Sobel(gray, mIntermediateMat, CvType.CV_8U, 1, 1)
        Core.convertScaleAbs(mIntermediateMat, mIntermediateMat, 10.0, 0.0)
        Imgproc.cvtColor(mIntermediateMat, mat, Imgproc.COLOR_GRAY2BGRA, 4)

        Imgproc.threshold(mat, mat, 127.0, 255.0, Imgproc.THRESH_BINARY_INV)

        mIntermediateMat.release()

        Utils.matToBitmap(mat, bitmap)

        return bitmap
    }



    // Función para aplicar la operación de cierre morfológico a una imagen.
    fun cerradura(bitmap: Bitmap): Bitmap {
        val mat = Mat() // Crea una nueva matriz OpenCV.
        Utils.bitmapToMat(bitmap, mat) // Convierte el bitmap de Android en una matriz OpenCV.

        val channels = ArrayList<Mat>() // Lista para almacenar los canales de color.
        Core.split(mat, channels) // Separa la imagen en sus canales de color.

        // Crea un elemento estructurante en forma de elipse para la operación morfológica.
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(9.0, 9.0))

        val cerraduraChannels = ArrayList<Mat>() // Lista para almacenar los canales procesados.
        // Aplica la operación de cierre a cada canal de color.
        for (channel in channels) {
            val cerraduraChannel = Mat() // Matriz para almacenar el resultado de cada canal.
            Imgproc.morphologyEx(channel, cerraduraChannel, Imgproc.MORPH_CLOSE, kernel) // Aplica cierre.
            cerraduraChannels.add(cerraduraChannel) // Añade el canal procesado a la lista.
        }

        val cerraduraMat = Mat() // Matriz para combinar los canales procesados.
        Core.merge(cerraduraChannels, cerraduraMat) // Combina los canales en una sola imagen.

        // Crea un bitmap para el resultado final.
        val resultBitmap = Bitmap.createBitmap(cerraduraMat.cols(), cerraduraMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(cerraduraMat, resultBitmap) // Convierte la matriz final de vuelta a un bitmap.

        return resultBitmap // Devuelve el bitmap procesado.
    }







    fun processImage(bitmap: Bitmap): Bitmap {
        // Convertir el Bitmap a Mat
        val img = Mat()
        Utils.bitmapToMat(bitmap, img)

        // Convertir a escala de grises
        val imgGray = Mat()
        Imgproc.cvtColor(img, imgGray, Imgproc.COLOR_BGR2GRAY)

        // Aplicar Canny para detectar bordes
        val imgCanny = Mat()
        Imgproc.Canny(imgGray, imgCanny, 10.0, 20.0)

        // Crear el kernel
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(13.0, 13.0))

        // Dilatar la imagen
        val imgDilate = Mat()
        Imgproc.dilate(imgCanny, imgDilate, kernel)

        // Erosionar la imagen
        val imgErode = Mat()
        Imgproc.erode(imgDilate, imgErode, kernel)

        // Encontrar los contornos
        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(imgErode, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_NONE)

        // Crear una máscara en blanco
        val mask = Mat.zeros(img.size(), CvType.CV_8U)

        // Dibujar los contornos en la máscara
        for (cnt in contours) {
            if (Imgproc.contourArea(cnt) > 500) {
                val peri = Imgproc.arcLength(MatOfPoint2f(*cnt.toArray()), true)
                val approx = MatOfPoint2f()
                Imgproc.approxPolyDP(MatOfPoint2f(*cnt.toArray()), approx, peri * 0.004, true)
                Imgproc.drawContours(mask, listOf(MatOfPoint(*approx.toArray())), -1, Scalar(255.0), -1)
            }
        }

        // Aplicar la máscara a la imagen original
        val imgMasked = Mat()
        img.copyTo(imgMasked, mask)

        // Convertir la Mat resultante a Bitmap
        val resultBitmap = Bitmap.createBitmap(imgMasked.cols(), imgMasked.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(imgMasked, resultBitmap)

        return resultBitmap
    }



    fun blurBackground(bitmap: Bitmap, faceCascade: CascadeClassifier): Bitmap {
        // Convertir el Bitmap a Mat
        val img = Mat()
        Utils.bitmapToMat(bitmap, img)
        val imgHeight = img.height()
        val imgWidth = img.width()

        // Convertir a escala de grises
        val imgGray = Mat()
        Imgproc.cvtColor(img, imgGray, Imgproc.COLOR_BGR2GRAY)

        // Detectar rostro(s)
        val faces = MatOfRect()
        faceCascade.detectMultiScale(imgGray, faces, 1.1, 5, 0, Size(30.0, 30.0), Size())

        // Crear una máscara en blanco
        val mask = Mat.zeros(img.size(), CvType.CV_8UC1)

        for (rect in faces.toArray()) {
            Imgproc.rectangle(mask, rect, Scalar(255.0), -1)
        }

        // Suavizar la máscara
        Imgproc.GaussianBlur(mask, mask, Size(15.0, 15.0), 0.0)

        // Invertir la máscara
        val invertedMask = Mat()
        Core.bitwise_not(mask, invertedMask)

        // Aplicar desenfoque gaussiano al fondo
        val blurred = Mat()
        Imgproc.GaussianBlur(img, blurred, Size(55.0, 55.0), 0.0)

        // Combinar la imagen original y la desenfocada utilizando la máscara
        val result = Mat()
        img.copyTo(result, mask)
        blurred.copyTo(result, invertedMask)

        // Convertir la Mat resultante a Bitmap
        val resultBitmap = Bitmap.createBitmap(result.cols(), result.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(result, resultBitmap)

        // Liberar recursos
        img.release()
        imgGray.release()
        mask.release()
        invertedMask.release()
        blurred.release()
        result.release()

        return resultBitmap
    }


    fun applyDogFilter(bitmap: Bitmap, cascadeClassifier: CascadeClassifier, context: Context): Bitmap {
        // Convertir bitmap a Mat y cambiar a BGRA para manejar alfa desde el principio
        val mat = Mat().apply {
            Utils.bitmapToMat(bitmap, this)
            Imgproc.cvtColor(this, this, Imgproc.COLOR_RGBA2BGRA)
        }

        // Cargar el filtro de perrito y convertirlo directamente a BGRA
        val dogBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.dog)
        val dogMat = Mat().apply {
            Utils.bitmapToMat(dogBitmap, this)
            Imgproc.cvtColor(this, this, Imgproc.COLOR_RGBA2BGRA)
        }

        // Detección de rostros en BGRA
        val faces = MatOfRect()
        cascadeClassifier.detectMultiScale(mat, faces, 1.1, 2, 2, Size(150.0, 150.0), Size())
        val facesArray = faces.toArray()

        // Aplicar filtro a cada cara detectada
        facesArray.forEach { face ->
            val dogResized = Mat()
            Imgproc.resize(dogMat, dogResized, Size((face.width * 1.5).toInt().toDouble(), (face.height * 1.95).toInt().toDouble()))

            val offsetX = (0.35 * face.width).toInt()
            val offsetY = (0.375 * face.height).toInt()
            val startX = face.x - offsetX
            val startY = face.y - offsetY

            for (i in 0 until dogResized.rows()) {
                for (j in 0 until dogResized.cols()) {
                    val pixel = dogResized.get(i, j)
                    if (pixel[3] > 20) { // Solo píxeles suficientemente opacos
                        val x = startX + j
                        val y = startY + i
                        if (y in 0 until mat.rows() && x in 0 until mat.cols()) {
                            mat.put(y, x, pixel[0], pixel[1], pixel[2], pixel[3]) // Incluir canal alfa
                        }
                    }
                }
            }
        }

        // Convertir de vuelta a Bitmap y asegurar el formato RGBA para la UI
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGB)
        Utils.matToBitmap(mat, bitmap)
        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2RGBA)
        return bitmap
    }


    fun getFeatures(bitmap: Bitmap, detector: Feature2D): Bitmap {
        val mat = Mat()
        val grayMat = Mat()
        val keypoints = MatOfKeyPoint()
        val descriptors = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)

        // Detect keypoints and compute descriptors
        detector.detectAndCompute(grayMat, Mat(), keypoints, descriptors)

        // Draw keypoints on the original image
        val outputMat = Mat()
        Features2d.drawKeypoints(mat, keypoints, outputMat, Scalar(51.0, 163.0, 236.0), Features2d.DrawMatchesFlags_NOT_DRAW_SINGLE_POINTS)

        // Convert the result back to Bitmap
        Utils.matToBitmap(outputMat, bitmap)

        // Release Mats
        mat.release()
        grayMat.release()
        keypoints.release()
        descriptors.release()
        outputMat.release()

        return bitmap
    }
    fun filterSIFT(bitmap: Bitmap): Bitmap {
        return try {
            val sift = SIFT.create()
            getFeatures(bitmap, sift)
        } catch (e: Exception) {
            bitmap  // Return unchanged bitmap in case of an error
        }
    }

    fun filterContours(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        val grayMat = Mat()
        val frame = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)

        // Make a copy of the original frame
        mat.copyTo(frame)

        // Create a reusable mask
        val mask = Mat.zeros(grayMat.size(), CvType.CV_8U)

        // Use various thresholds
        val thresholds = listOf(15, 50, 100, 240)
        for (threshold in thresholds) {
            val thresh = Mat()

            // Apply threshold
            Imgproc.threshold(grayMat, thresh, threshold.toDouble(), 255.0, Imgproc.THRESH_BINARY)

            // Find contours
            val contours = ArrayList<MatOfPoint>()
            val hierarchy = Mat()
            Imgproc.findContours(thresh, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)

            for (contour in contours) {
                // Reset mask to zero
                mask.setTo(Scalar(0.0))

                // Draw contour on mask
                Imgproc.drawContours(mask, listOf(contour), 0, Scalar(255.0), -1)

                // Find mean color inside mask
                val mean = Core.mean(mat, mask)

                // Draw contour with mean color on frame
                Imgproc.drawContours(frame, listOf(contour), 0, Scalar(mean.`val`[0], mean.`val`[1], mean.`val`[2], mean.`val`[3]), -1)
            }

            // Draw contours with black color
            Imgproc.drawContours(frame, contours, -1, Scalar(0.0, 0.0, 0.0), 1)

            // Release the threshold and hierarchy matrices
            thresh.release()
            hierarchy.release()
        }

        // Convert the result back to Bitmap
        Utils.matToBitmap(frame, bitmap)

        // Release Mats
        mat.release()
        grayMat.release()
        frame.release()
        mask.release()

        return bitmap
    }

    fun filterBlur(bitmap: Bitmap, blurType: String): Bitmap {
        val mat = Mat()
        val result = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, mat)

        when (blurType) {
            "gaussian" -> {
                // Gaussian blur
                Imgproc.GaussianBlur(mat, result, Size(29.0, 29.0), 0.0)
            }
            "median" -> {
                // Median blur
                Imgproc.medianBlur(mat, result, 29)
            }
            "classic" -> {
                // Classic blur
                Imgproc.blur(mat, result, Size(29.0, 29.0))
            }
            else -> {
                // Default to classic blur if unknown type
                Imgproc.blur(mat, result, Size(29.0, 29.0))
            }
        }

        // Convert the result back to Bitmap
        Utils.matToBitmap(result, bitmap)

        // Release Mats
        mat.release()
        result.release()

        return bitmap
    }

    private var previousFrame: Mat? = null

    fun filterMotion(bitmap: Bitmap): Bitmap {
        val currentFrame = Mat()
        val resultFrame = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, currentFrame)

        // Convert current frame to grayscale
        val grayCurrent = Mat()
        Imgproc.cvtColor(currentFrame, grayCurrent, Imgproc.COLOR_RGB2GRAY)

        if (previousFrame == null || previousFrame!!.size() != currentFrame.size()) {
            // Remember the previous frame
            previousFrame = currentFrame.clone()
            // Return unchanged bitmap if no previous frame exists or sizes do not match
            Utils.matToBitmap(currentFrame, bitmap)
            currentFrame.release()
            grayCurrent.release()
            return bitmap
        }

        // Convert previous frame to grayscale
        val grayPrevious = Mat()
        Imgproc.cvtColor(previousFrame, grayPrevious, Imgproc.COLOR_RGB2GRAY)

        // Get absolute difference between two frames
        Core.absdiff(grayCurrent, grayPrevious, resultFrame)

        // Update previous frame
        previousFrame!!.release()
        previousFrame = currentFrame.clone()

        // Convert the result back to Bitmap
        Utils.matToBitmap(resultFrame, bitmap)

        // Release Mats
        currentFrame.release()
        grayCurrent.release()
        grayPrevious.release()
        resultFrame.release()

        return bitmap
    }

    fun filterSkin(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        val hsvMat = Mat()
        val skinMask = Mat()
        val result = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, mat)

        // Convert to HSV
        Imgproc.cvtColor(mat, hsvMat, Imgproc.COLOR_RGB2HSV)

        // Define HSV range for skin tones
        val lower = Scalar(0.0, 100.0, 0.0)
        val upper = Scalar(50.0, 255.0, 255.0)

        // Create skin mask
        Core.inRange(hsvMat, lower, upper, skinMask)

        // Apply Gaussian blur to the mask
        Imgproc.GaussianBlur(skinMask, skinMask, Size(9.0, 9.0), 0.0)

        // Create kernel for morphology operation
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(4.0, 4.0))

        // Apply morphological CLOSE operation (dilate followed by erode)
        Imgproc.morphologyEx(skinMask, skinMask, Imgproc.MORPH_CLOSE, kernel, Point(-1.0, -1.0), 3)

        // Apply Gaussian blur again to the mask
        Imgproc.GaussianBlur(skinMask, skinMask, Size(9.0, 9.0), 0.0)

        // Apply the skin mask to the original image
        Core.bitwise_and(mat, mat, result, skinMask)

        // Convert the result back to Bitmap
        Utils.matToBitmap(result, bitmap)

        // Release Mats
        mat.release()
        hsvMat.release()
        skinMask.release()
        result.release()

        return bitmap
    }

    fun filterEqualize(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        val result = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, mat)

        // Split the image into its blue, green and red channels
        val channels = ArrayList<Mat>(3)
        Core.split(mat, channels)
        val b = channels[0]
        val g = channels[1]
        val r = channels[2]

        // Apply Histogram Equalization to each channel
        val bEqualized = Mat()
        val gEqualized = Mat()
        val rEqualized = Mat()
        Imgproc.equalizeHist(b, bEqualized)
        Imgproc.equalizeHist(g, gEqualized)
        Imgproc.equalizeHist(r, rEqualized)

        // Merge the equalized channels back into one image
        val equalizedChannels = ArrayList<Mat>(3)
        equalizedChannels.add(bEqualized)
        equalizedChannels.add(gEqualized)
        equalizedChannels.add(rEqualized)
        Core.merge(equalizedChannels, result)

        // Convert the result back to Bitmap
        Utils.matToBitmap(result, bitmap)

        // Release Mats
        mat.release()
        result.release()
        b.release()
        g.release()
        r.release()
        bEqualized.release()
        gEqualized.release()
        rEqualized.release()

        return bitmap
    }

    fun filterClahe(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        val result = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, mat)

        // Create a CLAHE object
        val clahe = Imgproc.createCLAHE(10.0, Size(8.0, 8.0))

        // Split the image into its blue, green and red channels
        val channels = ArrayList<Mat>(3)
        Core.split(mat, channels)
        val b = channels[0]
        val g = channels[1]
        val r = channels[2]

        // Apply CLAHE to each channel
        val bClahe = Mat()
        val gClahe = Mat()
        val rClahe = Mat()
        clahe.apply(b, bClahe)
        clahe.apply(g, gClahe)
        clahe.apply(r, rClahe)

        // Merge the CLAHE applied channels back into one image
        val claheChannels = ArrayList<Mat>(3)
        claheChannels.add(bClahe)
        claheChannels.add(gClahe)
        claheChannels.add(rClahe)
        Core.merge(claheChannels, result)

        // Convert the result back to Bitmap
        Utils.matToBitmap(result, bitmap)

        // Release Mats
        mat.release()
        result.release()
        b.release()
        g.release()
        r.release()
        bClahe.release()
        gClahe.release()
        rClahe.release()

        return bitmap
    }

    fun filterLab(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        val labMat = Mat()
        val result = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, mat)

        // Convert image to LAB color model
        Imgproc.cvtColor(mat, labMat, Imgproc.COLOR_RGB2Lab)

        // Split the image into its L, A and B channels
        val labChannels = ArrayList<Mat>(3)
        Core.split(labMat, labChannels)
        val l = labChannels[0]
        val a = labChannels[1]
        val b = labChannels[2]

        // Create a CLAHE object
        val clahe = Imgproc.createCLAHE(3.0, Size(8.0, 8.0))

        // Apply CLAHE to L-channel
        val lClahe = Mat()
        clahe.apply(l, lClahe)

        // Merge enhanced L-channel with the A and B channels
        val claheChannels = ArrayList<Mat>(3)
        claheChannels.add(lClahe)
        claheChannels.add(a)
        claheChannels.add(b)
        Core.merge(claheChannels, labMat)

        // Convert back to RGB color model
        Imgproc.cvtColor(labMat, result, Imgproc.COLOR_Lab2RGB)

        // Convert the result back to Bitmap
        Utils.matToBitmap(result, bitmap)

        // Release Mats
        mat.release()
        labMat.release()
        result.release()
        l.release()
        a.release()
        b.release()
        lClahe.release()

        return bitmap
    }

    fun filterSobelX(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        val grayMat = Mat()
        val sobelMat = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)

        // Apply Sobel filter with ksize = -1 (Scharr filter)
        Imgproc.Sobel(grayMat, sobelMat, CvType.CV_8U, 1, 0, -1)

        // Convert the result back to Bitmap
        val outputBitmap = Bitmap.createBitmap(sobelMat.cols(), sobelMat.rows(), bitmap.config)
        Utils.matToBitmap(sobelMat, outputBitmap)

        // Release Mats
        mat.release()
        grayMat.release()
        sobelMat.release()

        return outputBitmap
    }

    fun filterSobelY(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        val grayMat = Mat()
        val sobelMat = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)

        // Apply Sobel filter with ksize = -1 (Scharr filter) in the Y direction
        Imgproc.Sobel(grayMat, sobelMat, CvType.CV_8U, 0, 1, -1)

        // Convert the result back to Bitmap
        val outputBitmap = Bitmap.createBitmap(sobelMat.cols(), sobelMat.rows(), bitmap.config)
        Utils.matToBitmap(sobelMat, outputBitmap)

        // Release Mats
        mat.release()
        grayMat.release()
        sobelMat.release()

        return outputBitmap
    }

    fun filter3Bits(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        val result = Mat()
        val mask = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, mat)

        // Create a mask with the same size and type as the original image
        mask.create(mat.size(), mat.type())
        mask.setTo(Scalar(224.0, 224.0, 224.0)) // 224 = 11100000 in binary

        // Leave the first three bits
        Core.bitwise_and(mat, mask, result)

        // Convert the result back to Bitmap
        val outputBitmap = Bitmap.createBitmap(result.cols(), result.rows(), bitmap.config)
        Utils.matToBitmap(result, outputBitmap)

        // Release Mats
        mat.release()
        result.release()
        mask.release()

        return outputBitmap
    }

    fun filterMaxRgb(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        val result = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, mat)

        // Split the image into its BGR components
        val channels = ArrayList<Mat>(3)
        Core.split(mat, channels)
        val b = channels[0]
        val g = channels[1]
        val r = channels[2]

        // Find the maximum pixel intensity values for each (x, y)-coordinate
        val max1 = Mat()
        val max2 = Mat()
        Core.max(r, g, max1)
        Core.max(max1, b, max2)

        // Create masks where the channels are less than the maximum
        val maskR = Mat()
        val maskG = Mat()
        val maskB = Mat()
        Core.compare(r, max2, maskR, Core.CMP_LT)
        Core.compare(g, max2, maskG, Core.CMP_LT)
        Core.compare(b, max2, maskB, Core.CMP_LT)

        // Set all pixel values less than the maximum to zero
        r.setTo(Scalar(0.0), maskR)
        g.setTo(Scalar(0.0), maskG)
        b.setTo(Scalar(0.0), maskB)

        // Merge the channels back together and return the image
        val resultChannels = ArrayList<Mat>(3)
        resultChannels.add(b)
        resultChannels.add(g)
        resultChannels.add(r)
        Core.merge(resultChannels, result)

        // Convert the result back to Bitmap
        val outputBitmap = Bitmap.createBitmap(result.cols(), result.rows(), bitmap.config)
        Utils.matToBitmap(result, outputBitmap)

        // Release Mats
        mat.release()
        result.release()
        b.release()
        g.release()
        r.release()
        max1.release()
        max2.release()
        maskR.release()
        maskG.release()
        maskB.release()

        return outputBitmap
    }

    fun filterChaoticRgb(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        val result = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, mat)

        // Create random values for each channel
        val random = Random()
        val bShift = random.nextInt(256) - 128
        val gShift = random.nextInt(256) - 128
        val rShift = random.nextInt(256) - 128

        // Split the image into its BGR components
        val channels = ArrayList<Mat>(3)
        Core.split(mat, channels)
        val b = channels[0]
        val g = channels[1]
        val r = channels[2]

        // Convert channels to 32-bit signed integers
        val bInt = Mat()
        val gInt = Mat()
        val rInt = Mat()
        b.convertTo(bInt, CvType.CV_32SC1)
        g.convertTo(gInt, CvType.CV_32SC1)
        r.convertTo(rInt, CvType.CV_32SC1)

        // Add random values to each channel
        Core.add(bInt, Scalar(bShift.toDouble()), bInt)
        Core.add(gInt, Scalar(gShift.toDouble()), gInt)
        Core.add(rInt, Scalar(rShift.toDouble()), rInt)

        // Clip values to the range [0, 255]
        Core.max(bInt, Scalar(0.0), bInt)
        Core.min(bInt, Scalar(255.0), bInt)
        Core.max(gInt, Scalar(0.0), gInt)
        Core.min(gInt, Scalar(255.0), gInt)
        Core.max(rInt, Scalar(0.0), rInt)
        Core.min(rInt, Scalar(255.0), rInt)

        // Convert channels back to 8-bit unsigned integers
        bInt.convertTo(b, CvType.CV_8UC1)
        gInt.convertTo(g, CvType.CV_8UC1)
        rInt.convertTo(r, CvType.CV_8UC1)

        // Merge the channels back together and return the image
        val resultChannels = ArrayList<Mat>(3)
        resultChannels.add(b)
        resultChannels.add(g)
        resultChannels.add(r)
        Core.merge(resultChannels, result)

        // Convert the result back to Bitmap
        val outputBitmap = Bitmap.createBitmap(result.cols(), result.rows(), bitmap.config)
        Utils.matToBitmap(result, outputBitmap)

        // Release Mats
        mat.release()
        result.release()
        bInt.release()
        gInt.release()
        rInt.release()
        b.release()
        g.release()
        r.release()

        return outputBitmap
    }

    fun anonymizeFacePixelate(bitmap: Bitmap, blocks: Int = 5): Bitmap {
        val mat = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, mat)

        // Get the height and width of the image
        val h = mat.rows()
        val w = mat.cols()

        // Compute the steps in the x and y directions
        val xSteps = IntArray(blocks + 1) { i -> (i * w) / blocks }
        val ySteps = IntArray(blocks + 1) { i -> (i * h) / blocks }

        // Loop over the blocks in both the x and y direction
        for (i in 1 until ySteps.size) {
            for (j in 1 until xSteps.size) {
                // Compute the starting and ending (x, y)-coordinates for the current block
                val startX = xSteps[j - 1]
                val startY = ySteps[i - 1]
                val endX = xSteps[j]
                val endY = ySteps[i]

                // Extract the ROI using submat, compute the mean of the ROI,
                // and then draw a rectangle with the mean RGB values over the ROI in the original image
                val roi = mat.submat(startY, endY, startX, endX)
                val meanColor = Core.mean(roi)
                Imgproc.rectangle(mat, Point(startX.toDouble(), startY.toDouble()), Point(endX.toDouble(), endY.toDouble()), Scalar(meanColor.`val`[0], meanColor.`val`[1], meanColor.`val`[2]), -1)
                roi.release()
            }
        }

        // Convert the result back to Bitmap
        val outputBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), bitmap.config)
        Utils.matToBitmap(mat, outputBitmap)

        // Release the Mat
        mat.release()

        return outputBitmap
    }












































// Proximamente filtros nuveos

    fun grisImage(bitmap: Bitmap):Bitmap{
        val mat = Mat()

        Utils.bitmapToMat(bitmap, mat)

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)

        Imgproc.Laplacian(mat, mat, CvType.CV_8U)

        Utils.matToBitmap(mat, bitmap)

        return bitmap
    }
    fun detecFace2(bitmap: Bitmap, cascadeClassifier: CascadeClassifier?): Bitmap {
        var mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        var rects = MatOfRect()

        val rgb = Mat()
        Imgproc.cvtColor(mat, rgb, Imgproc.COLOR_RGBA2RGB)
        var gray = Mat()
        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_BGR2GRAY)

        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(gray, rects, 1.1, 2)
        }
        for(rect in rects.toList()){
            var submat: Mat = rgb.submat(rect)
            Imgproc.blur(submat, submat, Size(10.0, 10.0))
            Imgproc.rectangle(rgb, rect, Scalar(0.0, 255.0, 0.0), 10)
        }

        Utils.matToBitmap(rgb, bitmap)
        return bitmap
    }

    fun cannyFiltroBlanco(bitmap: Bitmap): Bitmap{

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val edges = Mat()

        Imgproc.Canny(mat, edges, 80.0, 100.0)

        Core.bitwise_not(edges, edges)

        Utils.matToBitmap(edges, bitmap)

        return bitmap
    }

    fun cambiarColorOjos(bitmap: Bitmap, faceCascade: CascadeClassifier, eyeCascade: CascadeClassifier): Bitmap {
        var mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        mat = CascadeRec3(mat, faceCascade, eyeCascade)

        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

    private fun CascadeRec3(mat: Mat, cascadeClassifier: CascadeClassifier, cascadeClassifier_eye: CascadeClassifier): Mat {
        val mRgb = Mat()
        Imgproc.cvtColor(mat, mRgb, Imgproc.COLOR_RGBA2RGB)

        val height = mRgb.height()
        val absoluteFaceSize = (height * 0.1).toDouble()

        val faces = MatOfRect()
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(
                mRgb, faces, 1.1, 2, 2,
                Size(absoluteFaceSize, absoluteFaceSize), Size()
            )
        }

        val facesArray = faces.toArray()
        for (face in facesArray) {
            Imgproc.rectangle(mat, face.tl(), face.br(), Scalar(0.0, 255.0, 0.0, 255.0), 2)

            val roi = Rect(face.tl().x.toInt(), face.tl().y.toInt(), face.width, face.height)
            val cropped = Mat(mat, roi)
            val eyes = MatOfRect()
            if (cascadeClassifier_eye != null) {
                cascadeClassifier_eye.detectMultiScale(
                    cropped, eyes, 1.15, 2,
                    Objdetect.CASCADE_FIND_BIGGEST_OBJECT or Objdetect.CASCADE_SCALE_IMAGE,
                    Size(35.0, 35.0), Size()
                )

                val eyesArray = eyes.toArray()
                for (eye in eyesArray) {
                    val x1 = eye.tl().x + face.tl().x
                    val y1 = eye.tl().y + face.tl().y
                    val x2 = eye.br().x + face.tl().x
                    val y2 = eye.br().y + face.tl().y

                    // Definir la región de interés (ROI) del ojo
                    val eyeROI = Mat(mat, Rect(Point(x1, y1), Point(x2, y2)))

                    // Convertir el ROI a HSV
                    val eyeHSV = Mat()
                    Imgproc.cvtColor(eyeROI, eyeHSV, Imgproc.COLOR_RGB2HSV)

                    // Dividir los canales HSV
                    val hsvChannels = ArrayList<Mat>(3)
                    Core.split(eyeHSV, hsvChannels)

                    // Cambiar el canal de tono (hue) para modificar el color a celeste
                    hsvChannels[0].setTo(Scalar(180.0)) // Ajusta este valor para tonos celestes (180-210)

                    // Unir los canales HSV
                    Core.merge(hsvChannels, eyeHSV)

                    // Convertir de vuelta a RGB
                    val eyeRGB = Mat()
                    Imgproc.cvtColor(eyeHSV, eyeRGB, Imgproc.COLOR_HSV2RGB)
                    Imgproc.cvtColor(eyeRGB, eyeROI, Imgproc.COLOR_RGB2RGBA)

                    // Dibujar un rectángulo alrededor del ojo
                    Imgproc.rectangle(mat, Point(x1, y1), Point(x2, y2), Scalar(0.0, 255.0, 0.0, 255.0), 2)
                }
            }
        }

        return mat
    }



    fun applyCanny(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val sizeMat = mat.size()
        val rows = sizeMat.height.toInt()
        val cols = sizeMat.width.toInt()

        val top = rows / 8
        val left = cols / 8
        val height = rows * 3 / 4
        val width = cols * 3 / 4

        val rgbaInnerWindow = mat.submat(top, top + height, left, left + width)

        val mIntermediateMat = Mat()
        Imgproc.Canny(rgbaInnerWindow, mIntermediateMat, 80.0, 90.0)
        Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4)

        rgbaInnerWindow.release()
        mIntermediateMat.release()

        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }
    fun cambiarColorIris(bitmap: Bitmap, eyeCascade: CascadeClassifier): Bitmap {
        var mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        mat = IrisColorChange(mat, eyeCascade)

        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

    private fun IrisColorChange(mat: Mat, eyeCascade: CascadeClassifier): Mat {
        val mRgb = Mat()
        Imgproc.cvtColor(mat, mRgb, Imgproc.COLOR_RGBA2RGB)

        val height = mRgb.height()
        val absoluteEyeSize: Double = height * 0.05

        val eyes = MatOfRect()
        if (eyeCascade != null) {
            eyeCascade.detectMultiScale(mRgb, eyes, 1.1, 2, 2, Size(absoluteEyeSize, absoluteEyeSize), Size())
        }

        val eyesArray: Array<Rect> = eyes.toArray()
        for (i in eyesArray.indices) {
            val eyeRect = eyesArray[i]
            val eyeROI = mat.submat(eyeRect)

            val center = Point(eyeROI.cols() / 2.0, eyeROI.rows() / 2.0)
            val radius = Math.min(eyeROI.cols(), eyeROI.rows()) / 4.0

            Imgproc.circle(eyeROI, center, radius.toInt(), Scalar(0.0, 0.0, 255.0), -1)
        }

        return mat
    }

    fun hitOrMiss(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        val channels = ArrayList<Mat>()
        Core.split(mat, channels)

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(6.0, 6.0))

        val cerraduraChannels = ArrayList<Mat>()
        for (channel in channels) {
            val cerraduraChannel = Mat()
            Imgproc.morphologyEx(channel, cerraduraChannel, Imgproc.MORPH_HITMISS, kernel)
            cerraduraChannels.add(cerraduraChannel)
        }
        val cerraduraMat = Mat()
        Core.merge(cerraduraChannels, cerraduraMat)

        val resultBitmap = Bitmap.createBitmap(cerraduraMat.cols(), cerraduraMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(cerraduraMat, resultBitmap)

        return resultBitmap
    }
    fun detectHarrisCorners(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        val matGray = Mat()
        val dest = Mat()
        val matResult = Mat()

        // Convert the bitmap to a Mat
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        Imgproc.cvtColor(mat, matGray, Imgproc.COLOR_RGB2GRAY)

        // Convert image to float32
        matGray.convertTo(matGray, CvType.CV_32F)

        // Harris corner detection
        Imgproc.cornerHarris(matGray, dest, 2, 5, 0.07)

        // Dilate Harris corner result to enhance the corners
        Imgproc.dilate(dest, dest, Mat())

        // Create a mask where corners are detected
        val mask = Mat()
        Core.compare(dest, Scalar(0.01 * Core.minMaxLoc(dest).maxVal), mask, Core.CMP_GT)

        // Convert original image to BGR (if needed)
        Imgproc.cvtColor(mat, matResult, Imgproc.COLOR_RGB2BGR)

        // Draw red corners on the original image
        matResult.setTo(Scalar(0.0, 0.0, 255.0), mask)

        // Convert the result back to Bitmap
        Utils.matToBitmap(matResult, bitmap)

        // Release Mats
        mat.release()
        matGray.release()
        dest.release()
        matResult.release()
        mask.release()

        return bitmap
    }
    private var affine: MutableMap<String, Any> = mutableMapOf()
    private val affineStart: MutableMap<String, Any> = mutableMapOf(
        "rotation" to 0.0,
        "shift" to mutableListOf(0, 0)
    )

    fun checkPrevious(currentFrame: Mat) {
        if (previousFrame == null || previousFrame!!.size() != currentFrame.size()) {
            previousFrame = currentFrame.clone()
            affine = HashMap(affineStart)  // deep copy starting affine values
        }
    }

    fun filterAffine1(bitmap: Bitmap): Bitmap {
        val currentFrame = Mat()
        val resultFrame = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, currentFrame)

        // Check previous frame for the first time
        checkPrevious(currentFrame)

        // Random rotation and shifts
        val rotation = affine["rotation"] as Double
        val shift = affine["shift"] as MutableList<Int>
        affine["rotation"] = rotation + listOf(-1, 1).random().toDouble()
        shift[0] += listOf(-1, 1).random()
        shift[1] += listOf(-1, 1).random()

        val rows = currentFrame.rows()
        val cols = currentFrame.cols()
        val halfX = cols / 2
        val halfY = rows / 2

        // Do not shift too far away
        shift[0] = max(shift[0], -halfX)
        shift[0] = min(shift[0], halfX)
        shift[1] = max(shift[1], -halfY)
        shift[1] = min(shift[1], halfY)

        // Rotation 2D matrix
        var m = Imgproc.getRotationMatrix2D(Point(halfX.toDouble(), halfY.toDouble()), affine["rotation"] as Double, 1.0)
        Imgproc.warpAffine(currentFrame, resultFrame, m, Size(cols.toDouble(), rows.toDouble()))

        // Shift matrix
        m = Mat(2, 3, CvType.CV_32F)
        m.put(0, 0, 1.0, 0.0, shift[0].toDouble())
        m.put(1, 0, 0.0, 1.0, shift[1].toDouble())
        Imgproc.warpAffine(resultFrame, resultFrame, m, Size(cols.toDouble(), rows.toDouble()))

        // Convert the result back to Bitmap
        Utils.matToBitmap(resultFrame, bitmap)

        // Release Mats
        currentFrame.release()
        resultFrame.release()

        return bitmap
    }


    fun filterLaplacian(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        val grayMat = Mat()
        val laplacianMat = Mat()
        val absLaplacianMat = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, mat)

        // Convert to grayscale
        Imgproc.cvtColor(mat, grayMat, Imgproc.COLOR_RGB2GRAY)

        // Apply Laplacian filter with depth CV_64F
        Imgproc.Laplacian(grayMat, laplacianMat, CvType.CV_64F)

        // Convert the result to absolute values and then to CV_8U
        Core.convertScaleAbs(laplacianMat, absLaplacianMat)

        // Convert the result back to Bitmap
        val outputBitmap = Bitmap.createBitmap(absLaplacianMat.cols(), absLaplacianMat.rows(), bitmap.config)
        Utils.matToBitmap(absLaplacianMat, outputBitmap)

        // Release Mats
        mat.release()
        grayMat.release()
        laplacianMat.release()
        absLaplacianMat.release()

        return outputBitmap
    }
    fun filterSwapRgb(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        val result = Mat()

        // Convert Bitmap to Mat
        Utils.bitmapToMat(bitmap, mat)

        // Split the image into its BGR components
        val channels = ArrayList<Mat>(3)
        Core.split(mat, channels)

        // Randomly shuffle color channels
        Collections.shuffle(channels)

        // Merge the shuffled channels back together and return the image
        Core.merge(channels, result)

        // Convert the result back to Bitmap
        val outputBitmap = Bitmap.createBitmap(result.cols(), result.rows(), bitmap.config)
        Utils.matToBitmap(result, outputBitmap)

        // Release Mats
        mat.release()
        result.release()
        for (channel in channels) {
            channel.release()
        }

        return outputBitmap
    }
}