package com.elegy.alicein4;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;

import java.util.Iterator;
import java.util.List;

public class AutoArFragment extends ArFragment
{
    private Activity activity;
    private ModelRenderable modelRenderable;
    private Anchor preAnchor = null;
    private Node preNode = null;
    private float northDegree = 0;
    private float routateDegree = 0;

    public void setActivity(Activity activity)
    {
        this.activity = activity;
    }

    public  void setRoutateDegree(float degree)
    {
        routateDegree = degree;
    }

    public Vector3 screenCenter()
    {
        View view = this.getArSceneView();
        return new Vector3(view.getWidth() / 2f, view.getHeight() / 2f, 0f);
    }
    /*
    @Override
    public void onUpdate(FrameTime frameTime)
    {
        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(activity, R.raw.scene)
                .build()
                .thenAccept(renderable -> modelRenderable = renderable)
                .exceptionally(
                        throwable ->
                        {
                            Toast toast = Toast.makeText(activity, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        super.onUpdate(frameTime);

        Frame frame = this.getArSceneView().getArFrame();
        if (frame != null)
        {
            //get the trackables to ensure planes are detected
            Iterator<Plane> var3 = frame.getUpdatedTrackables(Plane.class).iterator();
            while(var3.hasNext())
            {
                Plane plane = var3.next();
                //If a plane has been detected & is being tracked by ARCore
                if (plane.getTrackingState() == TrackingState.TRACKING)
                {
                    //Get all added anchors to the frame
                    Iterator<Anchor> iterableAnchor = frame.getUpdatedAnchors().iterator();
                    if (!iterableAnchor.hasNext())
                    {
                        degree = getDegree(39.875088, 116.482115, 39.876183, 116.482088);
                        Toast.makeText(activity, String.valueOf(degree), Toast.LENGTH_SHORT).show();
                        makeAr(plane, frame);
                    }
                }
            }
        }
    }

    public void makeAr(Plane plane, Frame frame)
    {
        for (int k = 0; k < 10; k++)
        {
            if (this.degree >= 160 && this.degree <= 170)
            {
                //Toast.makeText(activity, "walk", Toast.LENGTH_SHORT).show();        //test!!!!
                List<HitResult> hitTest = frame.hitTest(screenCenter().x, screenCenter().y);

                Iterator<HitResult> hitTestIterator = hitTest.iterator();

                while (hitTestIterator.hasNext())
                {
                    if (preAnchor != null)
                    {
                        preAnchor.detach();
                        Toast.makeText(activity, "detach the Anchor", Toast.LENGTH_LONG).show();       //test!!!!!!!!
                    }
                    HitResult hitResult = (HitResult) hitTestIterator.next();
                    Anchor modelAnchor = plane.createAnchor(hitResult.getHitPose());
                    preAnchor = modelAnchor;

                    AnchorNode anchorNode = new AnchorNode(modelAnchor);
                    anchorNode.setParent(this.getArSceneView().getScene());

                    TransformableNode transformableNode = new TransformableNode(this.getTransformationSystem());
                    transformableNode.setParent(anchorNode);
                    transformableNode.setRenderable(modelRenderable);

                    float x = modelAnchor.getPose().tx();
                    float y = modelAnchor.getPose().compose(Pose.makeTranslation(0f, 0f, 0)).ty();

                    transformableNode.setWorldRotation(new Quaternion(new Vector3(x, y, -k)));          //Vector3??
                }
            }
        }
    }

     */

    @Override
    public void onUpdate(FrameTime frameTime)
    {
        // When you build a Renderable, Sceneform loads its resources in the background while returning
        // a CompletableFuture. Call thenAccept(), handle(), or check isDone() before calling get().
        ModelRenderable.builder()
                .setSource(activity, R.raw.scene)
                .build()
                .thenAccept(renderable -> modelRenderable = renderable)
                .exceptionally(
                        throwable ->
                        {
                            Toast toast = Toast.makeText(activity, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        super.onUpdate(frameTime);

        Frame frame = this.getArSceneView().getArFrame();       // Get the frame from the scene for shorthand
        if (frame != null)
        {
            Iterator<Plane> var3 = frame.getUpdatedTrackables(Plane.class).iterator();      // Get the trackables to ensure planes are detected
            while(var3.hasNext())
            {
                Plane plane = var3.next();
                if (plane.getTrackingState() == TrackingState.TRACKING)     // If a plane has been detected & is being tracked by ARCore
                {
                    //this.getPlaneDiscoveryController().hide();      // Hide the plane discovery helper animation
                    Iterator<Anchor> iterableAnchor = frame.getUpdatedAnchors().iterator();     // Get all added anchors to the frame
                    if (!iterableAnchor.hasNext())      // Place the first object only if no previous anchors were added
                    {
                        List<HitResult> hitTest = frame.hitTest(screenCenter().x, screenCenter().y);    // Perform a hit test at the center of the screen to place an object without tapping
                        Iterator<HitResult> hitTestIterator = hitTest.iterator();       // Iterate through all hits
                        while(hitTestIterator.hasNext())
                        {
                            if (preAnchor != null)
                            {
                                preAnchor.detach();
                                //Toast.makeText(activity, "detach the Anchor", Toast.LENGTH_LONG).show();       //test!!!!!!!!
                            }
                            HitResult hitResult = hitTestIterator.next();
                            //Create an anchor at the plane hit
                            Anchor modelAnchor = plane.createAnchor(hitResult.getHitPose());
                            preAnchor = modelAnchor;
                            //Attach a node to this anchor with the scene as the parent
                            AnchorNode anchorNode = new AnchorNode(modelAnchor);
                            anchorNode.setParent(this.getArSceneView().getScene());
                            // Create a new Node that will carry our object
                            Node node = new Node();
                            node.setParent(anchorNode);
                            node.setRenderable(modelRenderable);
                            if (northDegree == 0)
                                northDegree = ((SimpleActivity) activity).getNorthDegree();
                            routateDegree = (float) ((SimpleActivity) activity).getCurrentDirect();
                            node.setWorldRotation(Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 180f + northDegree));
                            //Toast.makeText(activity, String.valueOf(routateDegree), Toast.LENGTH_LONG).show();
                            preNode = node;
                            //Toast.makeText(activity, String.valueOf(northDegree), Toast.LENGTH_LONG).show();       //test!!!!!!!!
                        }
                    }
                    else
                    {
                        //Toast.makeText(activity, "The update is" + preNode.getWorldRotation().toString(), Toast.LENGTH_LONG).show();       //test!!!!!!!!
                        routateDegree = (float) ((SimpleActivity) activity).getCurrentDirect();
                        preNode.setWorldRotation(Quaternion.axisAngle(new Vector3(0f, 1f, 0f), 180f + northDegree));
                    }
                }
            }
        }
    }
}


















