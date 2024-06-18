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
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.MatOfRect
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import org.opencv.objdetect.Objdetect
import kotlin.math.cos
import kotlin.math.sin

class OpenUtils {



    fun setUtil(bitmap: Bitmap):Bitmap{
        val mat = Mat()

        Utils.bitmapToMat(bitmap, mat)

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)

        Imgproc.Laplacian(mat, mat, CvType.CV_8U)

        Utils.matToBitmap(mat, bitmap)

        return bitmap
    }
    fun detectEdges(bitmap: Bitmap): Bitmap{

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val edges = Mat()

        Imgproc.Canny(mat, edges, 80.0, 200.0)

        val lines = Mat()

        val p1 = Point()
        val p2 = Point()

        var a: Double
        var b: Double
        var x0: Double
        var y0: Double

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

            Imgproc.line(mat, p1, p2, Scalar(255.0, 255.0, 255.0), 1, Imgproc.LINE_AA, 0)
        }

        Utils.matToBitmap(mat, bitmap)

        return bitmap
    }

    fun variableThreshold(bitmap: Bitmap): Bitmap {
        val blockSize = 10
        val c = 10

        val width = bitmap.width
        val height = bitmap.height

        val thresholdBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height step blockSize) {
            for (x in 0 until width step blockSize) {
                var blockSum = 0
                var pixelCount = 0

                for (j in y until minOf(y + blockSize, height)) {
                    for (i in x until minOf(x + blockSize, width)) {
                        val pixel = pixels[j * width + i]
                        val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                        blockSum += gray
                        pixelCount++
                    }
                }

                val blockMean = blockSum.toDouble() / pixelCount
                val threshold = blockMean - c

                for (j in y until minOf(y + blockSize, height)) {
                    for (i in x until minOf(x + blockSize, width)) {
                        val pixel = pixels[j * width + i]
                        val gray = (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
                        pixels[j * width + i] = if (gray > threshold) Color.WHITE else Color.BLACK
                    }
                }
            }
        }

        thresholdBitmap.setPixels(pixels, 0, width, 0, 0, width, height)

        return thresholdBitmap
    }

    fun detectFace(bitmap: Bitmap, cascadeClassifier: CascadeClassifier): Bitmap {
        var mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        mat = CascadeRec(mat, cascadeClassifier)



        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }
    private fun CascadeRec(mat: Mat, cascadeClassifier: CascadeClassifier): Mat{



        val mRgb = Mat()

        Imgproc.cvtColor(mat, mRgb, Imgproc.COLOR_RGBA2RGB)

        val height = mRgb.height()
        var absoluteFaceSize: Double = height * 0.1

        var faces: MatOfRect = MatOfRect()
        if(cascadeClassifier != null){
            cascadeClassifier.detectMultiScale(mRgb, faces, 1.1, 2, 2, Size(absoluteFaceSize, absoluteFaceSize), Size())
        }

        val facesArray: Array<Rect> = faces.toArray()

        for(i in facesArray.indices){
            var submat: Mat = mat.submat(facesArray[i])
            Imgproc.blur(submat, submat, Size(10.0, 10.0))
            Imgproc.rectangle(mat, facesArray[i].tl(), facesArray[i].br(), Scalar(0.0, 255.0, 0.0, 255.0), 2)
        }



        return mat
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

//        val rectArray: Array<Rect> = rects.toArray()
//
//        for(i in rectArray.indices){
//            Imgproc.rectangle(mat, facesArray[i].tl(), facesArray[i].br(), Scalar(0.0, 255.0, 0.0, 255.0), 2)
//        }
        for(rect in rects.toList()){
            var submat: Mat = rgb.submat(rect)
            Imgproc.blur(submat, submat, Size(10.0, 10.0))
            Imgproc.rectangle(rgb, rect, Scalar(0.0, 255.0, 0.0), 10)
        }

        Utils.matToBitmap(rgb, bitmap)
        return bitmap
    }
    fun cannyFiltro(bitmap: Bitmap): Bitmap{

        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val edges = Mat()

        Imgproc.Canny(mat, edges, 80.0, 200.0)

        Utils.matToBitmap(edges, bitmap)

        return bitmap
    }

    fun detectFaceEye(bitmap: Bitmap, cascadeClassifier: CascadeClassifier, cascadeClassifier_eye: CascadeClassifier): Bitmap {
        var mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        mat = CascadeRec2(mat, cascadeClassifier, cascadeClassifier_eye)



        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }
    private fun CascadeRec2(mat: Mat, cascadeClassifier: CascadeClassifier, cascadeClassifier_eye: CascadeClassifier): Mat{



        val mRgb = Mat()

        Imgproc.cvtColor(mat, mRgb, Imgproc.COLOR_RGBA2RGB)

        val height = mRgb.height()
        var absoluteFaceSize: Double = height * 0.1

        var faces: MatOfRect = MatOfRect()
        if(cascadeClassifier != null){
            cascadeClassifier.detectMultiScale(mRgb, faces, 1.1, 2, 2, Size(absoluteFaceSize, absoluteFaceSize), Size())
        }

        val facesArray: Array<Rect> = faces.toArray()

        for(i in facesArray.indices){
            Imgproc.rectangle(mat, facesArray[i].tl(), facesArray[i].br(), Scalar(0.0, 255.0, 0.0, 255.0), 2)

            var roi: Rect = Rect(facesArray[i].tl().x.toInt(), facesArray[i].tl().y.toInt(), facesArray[i].br().x.toInt() - facesArray[i].tl().x.toInt(),
                facesArray[i].br().y.toInt() - facesArray[i].tl().y.toInt())


            var cropped: Mat = Mat(mat, roi)
            val eyes: MatOfRect = MatOfRect()
            if(cascadeClassifier_eye != null){
                cascadeClassifier_eye.detectMultiScale(cropped, eyes, 1.15, 2,
                    Objdetect.CASCADE_FIND_BIGGEST_OBJECT or Objdetect.CASCADE_SCALE_IMAGE, Size(35.0, 35.0), Size())

                val eyesArray: Array<Rect> = eyes.toArray()

                for(j in eyesArray.indices){
                    var x1:Double = eyesArray[j].tl().x + facesArray[i].tl().x
                    var y1:Double = eyesArray[j].tl().y + facesArray[i].tl().y

                    var w1:Double = eyesArray[j].br().x - eyesArray[j].tl().x
                    var h1:Double = eyesArray[j].br().y - eyesArray[j].tl().y

                    var x2:Double = w1 + x1
                    var y2:Double = h1 + y1

                    Imgproc.rectangle(mat, Point(x1, y1), Point(x2, y2), Scalar(0.0, 255.0, 0.0, 255.0), 2)

                }
            }
        }



        return mat
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

//    fun applyPixelize(bitmap: Bitmap): Bitmap {
//        val mat = Mat()
//        Utils.bitmapToMat(bitmap, mat)
//
//        val sizeMat = mat.size()
//        val rows = sizeMat.height.toInt()
//        val cols = sizeMat.width.toInt()
//
//        val top = rows / 8
//        val left = cols / 8
//        val height = rows * 3 / 4
//        val width = cols * 3 / 4
//
//        val subMat = mat.submat(top, top + height, left, left + width)
//        val mIntermediateMat = Mat()
//
//        Imgproc.resize(subMat, mIntermediateMat, mIntermediateMat.size(), 0.1, 0.1, Imgproc.INTER_NEAREST)
//        Imgproc.resize(mIntermediateMat, subMat, subMat.size(), 0.0, 0.0, Imgproc.INTER_NEAREST)
//
//        subMat.release()
//        val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
//        Utils.matToBitmap(mat, resultBitmap)
//
//        mat.release()
//
//        return resultBitmap
//    }


    fun applyPixelize(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val mIntermediateMat = Mat()

        // Aplica el efecto de pixelización directamente a toda la imagen
        Imgproc.resize(mat, mIntermediateMat, mIntermediateMat.size(), 0.1, 0.1, Imgproc.INTER_NEAREST)
        Imgproc.resize(mIntermediateMat, mat, mat.size(), 0.0, 0.0, Imgproc.INTER_NEAREST)

        val resultBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, resultBitmap)

        mat.release()

        return resultBitmap
    }
//    fun applyPosterize(bitmap: Bitmap): Bitmap {
//        val mat = Mat()
//        Utils.bitmapToMat(bitmap, mat)
//
//        val sizeMat = mat.size()
//        val rows = sizeMat.height.toInt()
//        val cols = sizeMat.width.toInt()
//
//        val top = rows / 8
//        val left = cols / 8
//        val height = rows * 3 / 4
//        val width = cols * 3 / 4
//
//        val rgbaInnerWindow = mat.submat(top, top + height, left, left + width)
//
//        val mIntermediateMat = Mat()
//        Imgproc.Canny(rgbaInnerWindow, mIntermediateMat, 80.0, 90.0)
//        rgbaInnerWindow.setTo(Scalar(0.0, 0.0, 0.0, 255.0), mIntermediateMat)
//        Core.convertScaleAbs(rgbaInnerWindow, mIntermediateMat, 1.0 / 16.0, 0.0)
//        Core.convertScaleAbs(mIntermediateMat, rgbaInnerWindow, 16.0, 0.0)
//
//        mIntermediateMat.release()
//        rgbaInnerWindow.release()
//
//        Utils.matToBitmap(mat, bitmap)
//        return bitmap
//    }

    fun applyPosterize(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val mIntermediateMat = Mat()
        Imgproc.Canny(mat, mIntermediateMat, 80.0, 90.0)
        mat.setTo(Scalar(0.0, 0.0, 0.0, 255.0), mIntermediateMat)
        Core.convertScaleAbs(mat, mIntermediateMat, 1.0 / 16.0, 0.0)
        Core.convertScaleAbs(mIntermediateMat, mat, 16.0, 0.0)

        mIntermediateMat.release()

        Utils.matToBitmap(mat, bitmap)

        return bitmap
    }
    fun applyZoom(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val sizeMat = mat.size()
        val rows = sizeMat.height.toInt()
        val cols = sizeMat.width.toInt()

        val top = rows / 8
        val left = cols / 8
        val height = rows * 3 / 4
        val width = cols * 3 / 4

        val zoomCorner = mat.submat(0, rows / 2 - rows / 10, 0, cols / 2 - cols / 10)
        val mZoomWindow = mat.submat(rows / 2 - 9 * rows / 100, rows / 2 + 9 * rows / 100, cols / 2 - 9 * cols / 100, cols / 2 + 9 * cols / 100)

        Imgproc.resize(mZoomWindow, zoomCorner, zoomCorner.size(), 0.0, 0.0, Imgproc.INTER_LINEAR_EXACT)

        val wsize = mZoomWindow.size()
        Imgproc.rectangle(mZoomWindow, Point(1.0, 1.0), Point(wsize.width - 2.0, wsize.height - 2.0), Scalar(255.0, 0.0, 0.0, 255.0), 2)

        zoomCorner.release()
        mZoomWindow.release()

        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

//    fun applySepia(bitmap: Bitmap): Bitmap {
//        val mat = Mat()
//        Utils.bitmapToMat(bitmap, mat)
//        var mSepiaKernel: Mat = Mat(4, 4, CvType.CV_32F)
//        mSepiaKernel = Mat(4, 4, CvType.CV_32F)
//        mSepiaKernel.put(0, 0, 0.189, 0.769, 0.393, 0.0)  // R
//        mSepiaKernel.put(1, 0, 0.168, 0.686, 0.349, 0.0)  // G
//        mSepiaKernel.put(2, 0, 0.131, 0.534, 0.272, 0.0)  // B
//        mSepiaKernel.put(3, 0, 0.0, 0.0, 0.0, 1.0)         // A
//
//        val sizeMat = mat.size()
//        val rows = sizeMat.height.toInt()
//        val cols = sizeMat.width.toInt()
//
//        val top = rows / 8
//        val left = cols / 8
//        val height = rows * 3 / 4
//        val width = cols * 3 / 4
//
//        val rgbaInnerWindow = mat.submat(top, top + height, left, left + width)
//
//        // Define el kernel de sepia aquí
//
//        Core.transform(rgbaInnerWindow, rgbaInnerWindow, mSepiaKernel)
//
//        rgbaInnerWindow.release()
//
//        Utils.matToBitmap(mat, bitmap)
//        return bitmap
//    }

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
//    fun applySobel(bitmap: Bitmap): Bitmap {
//        val mat = Mat()
//        Utils.bitmapToMat(bitmap, mat)
//
//        val sizeMat = mat.size()
//        val rows = sizeMat.height.toInt()
//        val cols = sizeMat.width.toInt()
//
//        val top = rows / 8
//        val left = cols / 8
//        val height = rows * 3 / 4
//        val width = cols * 3 / 4
//
//        val gray = Mat()
//        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
//
//        val grayInnerWindow = gray.submat(top, top + height, left, left + width)
//        val rgbaInnerWindow = mat.submat(top, top + height, left, left + width)
//
//        val mIntermediateMat = Mat()
//        Imgproc.Sobel(grayInnerWindow, mIntermediateMat, CvType.CV_8U, 1, 1)
//        Core.convertScaleAbs(mIntermediateMat, mIntermediateMat, 10.0, 0.0)
//        Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4)
//
//        grayInnerWindow.release()
//        rgbaInnerWindow.release()
//        mIntermediateMat.release()
//
//        Utils.matToBitmap(mat, bitmap)
//        return bitmap
//    }

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

//    fun cerradura(bitmap: Bitmap): Bitmap {
//        val mat = Mat()
//        Utils.bitmapToMat(bitmap, mat)
//
//        val gray = Mat()
//        Imgproc.cvtColor(mat, gray, Imgproc.COLOR_RGBA2GRAY)
//
//        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(9.0, 9.0))
//
//        val cerradura = Mat()
//        Imgproc.morphologyEx(gray, cerradura, Imgproc.MORPH_CLOSE, kernel)
//
//        Utils.matToBitmap(cerradura, bitmap)
//
//        return bitmap
//    }

    fun cerradura(bitmap: Bitmap): Bitmap {
        val mat = Mat()
        Utils.bitmapToMat(bitmap, mat)

        val channels = ArrayList<Mat>()
        Core.split(mat, channels)

        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(9.0, 9.0))

        val cerraduraChannels = ArrayList<Mat>()
        for (channel in channels) {
            val cerraduraChannel = Mat()
            Imgproc.morphologyEx(channel, cerraduraChannel, Imgproc.MORPH_CLOSE, kernel)
            cerraduraChannels.add(cerraduraChannel)
        }

        val cerraduraMat = Mat()
        Core.merge(cerraduraChannels, cerraduraMat)

        val resultBitmap = Bitmap.createBitmap(cerraduraMat.cols(), cerraduraMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(cerraduraMat, resultBitmap)

        return resultBitmap
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


















//    fun applyDogFilter(bitmap: Bitmap, cascadeClassifier: CascadeClassifier, context: Context): Bitmap {
//        // Convertir bitmap a Mat y cambiar a BGRA para manejar alfa desde el principio
//        val mat = Mat().apply {
//            Utils.bitmapToMat(bitmap, this)
//            Imgproc.cvtColor(this, this, Imgproc.COLOR_RGBA2BGRA)
//        }
//
//        // Cargar el filtro de perrito y convertirlo directamente a BGRA
//        val dogBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.dog)
//        val dogMat = Mat().apply {
//            Utils.bitmapToMat(dogBitmap, this)
//            Imgproc.cvtColor(this, this, Imgproc.COLOR_RGBA2BGRA)
//        }
//
//        // Detección de rostros en BGRA
//        val faces = MatOfRect()
//        cascadeClassifier.detectMultiScale(mat, faces, 1.1, 2, 2, Size(150.0, 150.0), Size())
//        val facesArray = faces.toArray()
//
//        // Aplicar filtro a cada cara detectada
//        facesArray.forEach { face ->
//            val dogResized = Mat()
//            Imgproc.resize(dogMat, dogResized, Size((face.width * 1.5).toInt().toDouble(), (face.height * 1.95).toInt().toDouble()))
//
//            val offsetX = (0.35 * face.width).toInt()
//            val offsetY = (0.375 * face.height).toInt()
//            val startX = face.x - offsetX
//            val startY = face.y - offsetY
//
//            for (i in 0 until dogResized.rows()) {
//                for (j in 0 until dogResized.cols()) {
//                    val pixel = dogResized.get(i, j)
//                    if (pixel[3] > 20) { // Solo píxeles suficientemente opacos
//                        val x = startX + j
//                        val y = startY + i
//                        if (y in 0 until mat.rows() && x in 0 until mat.cols()) {
//                            mat.put(y, x, pixel[0], pixel[1], pixel[2], pixel[3]) // Incluir canal alfa
//                        }
//                    }
//                }
//            }
//        }
//
//        // Convertir de vuelta a Bitmap y asegurar el formato RGBA para la UI
//        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2RGB)
//        Utils.matToBitmap(mat, bitmap)
//        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2RGBA)
//        return bitmap
//    }



//    fun applyDogFilter(bitmap: Bitmap, cascadeClassifier: CascadeClassifier, context: Context): Bitmap {
//        // Convertir bitmap a Mat y cambiar a BGRA para manejar alfa desde el principio
//        val mat = Mat().apply {
//            Utils.bitmapToMat(bitmap, this)
//            Imgproc.cvtColor(this, this, Imgproc.COLOR_RGBA2BGRA)
//        }
//
//        // Cargar el filtro de perrito y convertirlo directamente a BGRA
//        val dogBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.dog)
//        val dogMat = Mat().apply {
//            Utils.bitmapToMat(dogBitmap, this)
//            Imgproc.cvtColor(this, this, Imgproc.COLOR_RGBA2BGRA)
//        }
//
//        // Detección de rostros en BGRA
//        val faces = MatOfRect()
//        cascadeClassifier.detectMultiScale(mat, faces, 1.1, 2, 2, Size(150.0, 150.0), Size())
//        val facesArray = faces.toArray()
//
//        // Aplicar filtro a cada cara detectada
//        facesArray.forEach { face ->
//            val dogResized = Mat()
//            Imgproc.resize(dogMat, dogResized, Size((face.width * 1.5).toInt().toDouble(), (face.height * 1.95).toInt().toDouble()))
//
//            val offsetX = (0.35 * face.width).toInt()
//            val offsetY = (0.375 * face.height).toInt()
//            val startX = maxOf(face.x - offsetX, 0)
//            val startY = maxOf(face.y - offsetY, 0)
//
//            val roi = mat.submat(Rect(startX, startY, minOf(dogResized.cols(), mat.cols() - startX), minOf(dogResized.rows(), mat.rows() - startY)))
//
//            // Crear una máscara a partir del canal alfa del filtro
//            val dogMask = Mat()
//            Core.extractChannel(dogResized, dogMask, 3)
//
//            // Invertir la máscara
//            val dogMaskInv = Mat()
//            Core.bitwise_not(dogMask, dogMaskInv)
//
//            // Aplicar la máscara invertida a la ROI del fondo
//            val background = Mat()
//            Core.bitwise_and(roi, roi, background, dogMaskInv)
//
//            // Aplicar la máscara del filtro al filtro
//            val foreground = Mat()
//            Core.bitwise_and(dogResized, dogResized, foreground, dogMask)
//
//            // Sumar el fondo y el filtro
//            val combined = Mat()
//            Core.add(background, foreground, combined)
//
//            // Copiar el resultado de vuelta a la imagen original
//            combined.copyTo(roi)
//        }
//
//        // Convertir de vuelta a Bitmap y asegurar el formato RGBA para la UI
//        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2RGBA)
//        Utils.matToBitmap(mat, bitmap)
//        return bitmap
//    }

//    fun applyDogFilter(bitmap: Bitmap, cascadeClassifier: CascadeClassifier, context: Context): Bitmap {
//        // Convertir bitmap a Mat y cambiar a BGRA para manejar alfa desde el principio
//        val mat = Mat().apply {
//            Utils.bitmapToMat(bitmap, this)
//            Imgproc.cvtColor(this, this, Imgproc.COLOR_RGBA2BGRA)
//        }
//
//        // Cargar el filtro de perrito y convertirlo directamente a BGRA
//        val dogBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.dog)
//        val dogMat = Mat().apply {
//            Utils.bitmapToMat(dogBitmap, this)
//            Imgproc.cvtColor(this, this, Imgproc.COLOR_RGBA2BGRA)
//        }
//
//        // Detección de rostros en BGRA
//        val faces = MatOfRect()
//        cascadeClassifier.detectMultiScale(mat, faces, 1.1, 2, 2, Size(150.0, 150.0), Size())
//        val facesArray = faces.toArray()
//
//        // Aplicar filtro a cada cara detectada
//        facesArray.forEach { face ->
//            val dogResized = Mat()
//            Imgproc.resize(dogMat, dogResized, Size((face.width * 1.5).toInt().toDouble(), (face.height * 1.95).toInt().toDouble()))
//
//            val offsetX = (0.35 * face.width).toInt()
//            val offsetY = (0.375 * face.height).toInt()
//            val startX = maxOf(face.x - offsetX, 0)
//            val startY = maxOf(face.y - offsetY, 0)
//
//            val endX = minOf(startX + dogResized.cols(), mat.cols())
//            val endY = minOf(startY + dogResized.rows(), mat.rows())
//
//            val width = endX - startX
//            val height = endY - startY
//
//            if (width > 0 && height > 0) {
//                val roi = mat.submat(Rect(startX, startY, width, height))
//                val dogRegion = dogResized.submat(Rect(0, 0, width, height))
//
//                // Crear una máscara a partir del canal alfa del filtro
//                val dogMask = Mat()
//                Core.extractChannel(dogRegion, dogMask, 3)
//
//                // Invertir la máscara
//                val dogMaskInv = Mat()
//                Core.bitwise_not(dogMask, dogMaskInv)
//
//                // Aplicar la máscara invertida a la ROI del fondo
//                val background = Mat(roi.size(), roi.type())
//                roi.copyTo(background, dogMaskInv)
//
//                // Aplicar la máscara del filtro al filtro
//                val foreground = Mat(dogRegion.size(), dogRegion.type())
//                dogRegion.copyTo(foreground, dogMask)
//
//                // Sumar el fondo y el filtro
//                Core.add(background, foreground, roi)
//            }
//        }
//
//        // Convertir de vuelta a Bitmap y asegurar el formato RGBA para la UI
//        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2RGBA)
//        Utils.matToBitmap(mat, bitmap)
//        return bitmap
//    }
//fun applyDogFilter(bitmap: Bitmap, cascadeClassifier: CascadeClassifier, context: Context): Bitmap {
//    val mat = Mat()
//    Utils.bitmapToMat(bitmap, mat)
//    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_RGBA2BGRA)
//
//    val dogBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.dog)
//    val dogMat = Mat()
//    Utils.bitmapToMat(dogBitmap, dogMat)
//    Imgproc.cvtColor(dogMat, dogMat, Imgproc.COLOR_RGBA2BGRA)
//
//    val faces = MatOfRect()
//    cascadeClassifier.detectMultiScale(mat, faces, 1.1, 2, 2, Size(150.0, 150.0), Size())
//    val facesArray = faces.toArray()
//
//    val dogResized = Mat()
//    facesArray.forEach { face ->
//        Imgproc.resize(dogMat, dogResized, Size((face.width * 1.5).toInt().toDouble(), (face.height * 1.95).toInt().toDouble()))
//
//        val offsetX = (0.35 * face.width).toInt()
//        val offsetY = (0.375 * face.height).toInt()
//        val startX = face.x - offsetX
//        val startY = face.y - offsetY
//
//        for (i in 0 until dogResized.rows()) {
//            for (j in 0 until dogResized.cols()) {
//                val pixel = dogResized.get(i, j)
//                if (pixel[3] > 20) {
//                    val x = startX + j
//                    val y = startY + i
//                    if (y in 0 until mat.rows() && x in 0 until mat.cols()) {
//                        mat.put(y, x, pixel[0], pixel[1], pixel[2], pixel[3])
//                    }
//                }
//            }
//        }
//    }
//
//    Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGRA2RGBA)
//    Utils.matToBitmap(mat, bitmap)
//    return bitmap
//}

    fun applyDogFilter(bitmap: Bitmap, cascadeClassifier: CascadeClassifier, context: Context): Bitmap {
        var mat = Mat()
        Utils.bitmapToMat(bitmap, mat)
        mat = CascadeRecado(mat, cascadeClassifier, context)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

    private fun CascadeRecado(mat: Mat, cascadeClassifier: CascadeClassifier, context: Context): Mat {
        val mRgb = Mat()
        Imgproc.cvtColor(mat, mRgb, Imgproc.COLOR_RGBA2RGB)

        val height = mRgb.height()
        var absoluteFaceSize: Double = height * 0.1

        var faces: MatOfRect = MatOfRect()
        if(cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(mRgb, faces, 1.1, 2, 2, Size(absoluteFaceSize, absoluteFaceSize), Size())
        }

        val facesArray: Array<Rect> = faces.toArray()
        val dogBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.dog)
        val dogMat = Mat()
        Utils.bitmapToMat(dogBitmap, dogMat)
        Imgproc.cvtColor(dogMat, dogMat, Imgproc.COLOR_RGBA2RGB)

        for(i in facesArray.indices) {
            val face = facesArray[i]
            val dogResized = Mat()
            Imgproc.resize(dogMat, dogResized, Size(face.width.toDouble(), face.height.toDouble()))

            dogResized.copyTo(mat.submat(face))
        }

        return mat
    }


}