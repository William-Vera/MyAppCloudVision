package com.example.myappcloudvision;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.BoundingPoly;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.FaceAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.TextAnnotation;
import com.google.api.services.vision.v1.model.Vertex;

import org.apache.commons.io.output.ByteArrayOutputStream;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    Vision vision;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Vision.Builder visionBuilder = new Vision.Builder(new NetHttpTransport(),
                new AndroidJsonFactory(), null);
        visionBuilder.setVisionRequestInitializer(new VisionRequestInitializer("AIzaSyCZ08ZGZLtgd4ZHQRQqAEuL-RJ10zIybxI"));

        vision = visionBuilder.build();
    }
    public void OpenGallery(View view) {
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }
    private static int RESULT_LOAD_IMAGE = 1;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data)
        {
            ImageView imageView = (ImageView) findViewById(R.id.imagen);
            imageView.setImageURI(data.getData());
        }
    }
    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxDimension) {
        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxDimension;
        int resizedHeight = maxDimension;
        if (originalHeight > originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxDimension;
            resizedWidth = maxDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }
    public Image getImageToProcess(){
        ImageView imagen = (ImageView)findViewById(R.id.imagen);
        BitmapDrawable drawable = (BitmapDrawable) imagen.getDrawable();
        Bitmap bitmap = drawable.getBitmap();
        //bitmap = scaleBitmapDown(bitmap, 800);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] imageInByte = stream.toByteArray();
        Image inputImage = new Image();
        inputImage.encodeContent(imageInByte);
        return inputImage;
    }

    public BatchAnnotateImagesRequest setBatchRequest(String TipoSolic, Image inputImage){
        Feature desiredFeature = new Feature();
        desiredFeature.setType(TipoSolic);
        AnnotateImageRequest request = new AnnotateImageRequest();
        request.setImage(inputImage);
        request.setFeatures(Arrays.asList(desiredFeature));
        BatchAnnotateImagesRequest batchRequest = new BatchAnnotateImagesRequest();
        batchRequest.setRequests(Arrays.asList(request));
        return batchRequest;
    }

    public void ProcesarTexto(View View){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BatchAnnotateImagesRequest batchRequest = setBatchRequest("TEXT_DETECTION", getImageToProcess());
                try {
                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    final TextAnnotation text = response.getResponses().get(0).getFullTextAnnotation();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView)findViewById(R.id.textView2);
                            imageDetail.setText(text.getText());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void ProcesarImagen(View View){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BatchAnnotateImagesRequest batchRequest = setBatchRequest("LABEL_DETECTION", getImageToProcess());
                try {
                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();

                    final StringBuilder message = new StringBuilder("Se ha encontrado los siguientes Objetos:\n\n");
                    List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
                    if (labels != null) {
                        for (EntityAnnotation label : labels)
                            message.append(String.format(Locale.US, "%.2f: %s\n", label.getScore()*100, label.getDescription()));
                    } else {
                        message.append("No hay ningún Objeto");
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView)findViewById(R.id.textView2);
                            imageDetail.setText(message.toString());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
    public void ProcesarRostro(View View){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                BatchAnnotateImagesRequest batchRequest = setBatchRequest("FACE_DETECTION", getImageToProcess());
                try {
                    Vision.Images.Annotate annotateRequest = vision.images().annotate(batchRequest);
                    annotateRequest.setDisableGZipContent(true);
                    BatchAnnotateImagesResponse response = annotateRequest.execute();

                    List<FaceAnnotation> faces = response.getResponses().get(0).getFaceAnnotations();
                    int numberOfFaces = faces.size();
                    String likelihoods = "";
                    FaceAnnotation cara;

                    ImageView image=findViewById(R.id.imagen);
                    BitmapDrawable drawable=(BitmapDrawable) image.getDrawable();
                    Bitmap bitmap=drawable.getBitmap().copy(Bitmap.Config.ARGB_8888, true);


                    //Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.id.imagen);
                    //Bitmap bitmapWithRectangles = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    Canvas canvas = new Canvas(bitmap);
                    //Canvas canvas = new Canvas(bitmap);

                    Paint paint= new Paint();
                    paint.setColor(Color.RED);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(5);
                    float x,y,z;
                    Vertex vertice;
                    FaceAnnotation rostro;
                    List<Vertex> vertices;

                    for(int i=0; i<numberOfFaces; i++) {
                        rostro=faces.get(i);
                        /*
                        for(int j=0;j<rostro.getBoundingPoly().getVertices().size(); j++){
                            vertice=rostro.getBoundingPoly().getVertices().get(j);

                            if(vertice.getX() !=null && vertice.getY()!=null){
                                canvas.drawPoint(vertice.getX(), vertice.getY(), paint);
                            }
                        }*/


                        likelihoods += "\n Rostro " + i + " " + faces.get(i).getJoyLikelihood();


                        //cara = faces.get(i);
                        BoundingPoly boundingPoly = rostro.getBoundingPoly();
                        vertices = boundingPoly.getVertices();
                        if (vertices.size() == 4) {
                            int left = vertices.get(0).getX();
                            int top = vertices.get(0).getY();
                            int right = vertices.get(2).getX();
                            int bottom = vertices.get(2).getY();
                            Rect rect = new Rect(left, top, right, bottom);
                            canvas.drawRect(rect, paint);
                        }

                    }
                    image.setImageBitmap(bitmap);
                    final String message = "Esta imagen tiene " + numberOfFaces + " rostros " + likelihoods;

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            TextView imageDetail = (TextView)findViewById(R.id.textView2);
                            imageDetail.setText(message.toString());
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}