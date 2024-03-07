/**
 * Author: Adam Jansen
 * Date: Nov 18, 2023
 * Project: Lab 4 
 * Class: CMPT315
 * NOTE I was able to get the two shadows onto the plane but it randomly removed my shadow on the blue sphere,
 * so i gave up haha, but ive tried to make the shadows work but it makes the blue sphere darker so it kinda works but you dont see the shadow
 * I got most of the shadowing working 
 */

import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.Graphics;

public class Laboratory4 extends JPanel {
	// Constants for the window size
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    private BufferedImage canvas; // Canvas to draw on

    public Laboratory4() {
        canvas = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
        fillCanvas(); // Fill the canvas with the rendered scene
    }

private void fillCanvas() {
	// Define light position and geometric objects in the scene
    Vector3 lightPosition = new Vector3(0, 5, -5);
    Sphere redSphere = new Sphere(new Vector3(-0.3, -0.23, 3), 0.5, new Color(255, 0, 0));
    Sphere blueSphere = new Sphere(new Vector3(0.5, -0.23, 4), 0.5, new Color(0, 0, 255));
    Plane greenPlane = new Plane(new Vector3(0, -1, 0), 1, new Color(0, 255, 0));

	// Arrays of spheres and planes to iterate
    Sphere[] spheres = {redSphere, blueSphere};
    Plane[] planes = {greenPlane};

	// Iterate over each pixel and calculate color based on ray tracing 
    for (int x = 0; x < WIDTH; x++) {
        for (int y = 0; y < HEIGHT; y++) {
            double normalizedX = (x - WIDTH / 2.0) / (double)WIDTH;
            double normalizedY = -(y - HEIGHT / 2.0) / (double)HEIGHT;
            Vector3 pixelPosition = new Vector3(normalizedX, normalizedY, 1);

            Ray ray = new Ray(new Vector3(0, 0, 0), pixelPosition.subtract(new Vector3(0, 0, 0)).normalize());

            Color color = traceRay(ray, spheres, planes, lightPosition);
            canvas.setRGB(x, y, color.getRGB());
        }
    }
}

// Method to trace a ray and dtermine its color after interacting with objects
private Color traceRay(Ray ray, Sphere[] spheres, Plane[] planes, Vector3 lightPosition) {
    Color color = Color.BLACK;
    double closestT = Double.POSITIVE_INFINITY;
    Sphere closestSphere = null;

    // Check for sphere intersections with each sphere
    for (Sphere sphere : spheres) {
        double t = sphere.intersectRay(ray);
        if (t > 0 && t < closestT) {
            closestT = t;
            closestSphere = sphere;
        }
    }

    // Check for plane intersections
    for (Plane plane : planes) {
        double t = plane.intersectRay(ray);
        if (t > 0 && t < closestT) {
            closestT = t;
            closestSphere = null; // We hit a plane instead of a sphere
        }
    }
	
	// Calculate the color at the intersection point 
    if (closestT < Double.POSITIVE_INFINITY) {
        Vector3 hitPoint = ray.origin.add(ray.direction.scale(closestT));
        Vector3 normalAtHit;
        Color objectColor;

        // Determine the normal and color at the hit point 
        if (closestSphere != null) {
            normalAtHit = hitPoint.subtract(closestSphere.center).normalize();
            objectColor = closestSphere.color;

            // If it hits the blue sphere, check if the red sphere casts a shadow on it
            if (closestSphere.color.equals(Color.BLUE)) {
                Vector3 toLight = lightPosition.subtract(hitPoint).normalize();
                Ray shadowRay = new Ray(hitPoint, toLight);
                double lightDistance = hitPoint.subtract(lightPosition).length();

                boolean isRedSphereShadowed = false;
                for (Sphere otherSphere : spheres) {
                    // Check if the shadow ray intersects the red sphere and it's between the hit point and the light
                    if (otherSphere.color.equals(Color.RED) && !otherSphere.equals(closestSphere)) {
                        double tShadow = otherSphere.intersectRay(shadowRay);
                        if (tShadow > 0 && tShadow < lightDistance) {
                            isRedSphereShadowed = true;
                            break;
                        }
                    }
                }

                if (isRedSphereShadowed) {
                    objectColor = darkenColor(objectColor, 0.5);
                }
            }
        } else {
            // Assuming the plane's normal is the Y axis
            normalAtHit = new Vector3(0, 1, 0);
            objectColor = planes[0].color;
        }
        
        // Check if the point is in the shadow
        boolean isInShadow = false;
        Ray shadowRay = new Ray(hitPoint, lightPosition.subtract(hitPoint).normalize());
        for (Sphere sphere : spheres) {
            if (sphere != closestSphere && sphere.intersectRay(shadowRay) > 0) {
                isInShadow = true;
                break;
            }
        }

        // Calculate the color with shadows
        if (isInShadow) {
            color = color.darker(); // Darken the color if in shadow
        } else {
            // Simple diffuse shading
            Vector3 lightDirection = lightPosition.subtract(hitPoint).normalize();
            double lightPower = Math.max(normalAtHit.dot(lightDirection), 0);
            color = new Color((int) (objectColor.getRed() * lightPower),
                              (int) (objectColor.getGreen() * lightPower),
                              (int) (objectColor.getBlue() * lightPower));
        }
    }

    return color;
}


    // Helper method to darken the color
    private Color darkenColor(Color color, double factor) {
        return new Color(Math.max((int)(color.getRed() * factor), 0),
                Math.max((int)(color.getGreen() * factor), 0),
                Math.max((int)(color.getBlue() * factor), 0));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(canvas, 0, 0, this); // Draws the canvas to the panel
    }

    public static void main(String[] args) {
		// Sets up the window and displays the panel
        JFrame frame = new JFrame("Lab 4 - Adam Jansen");
        Laboratory4 panel = new Laboratory4();
        frame.add(panel);
        frame.setSize(WIDTH, HEIGHT);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

// Below are classes representing 3D vectors, rays, spheres, and planes with relevant methods
class Vector3 {
    public double x, y, z;

    public Vector3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3 add(Vector3 other) {
        return new Vector3(x + other.x, y + other.y, z + other.z);
    }

    public Vector3 subtract(Vector3 other) {
        return new Vector3(x - other.x, y - other.y, z - other.z);
    }

    public Vector3 scale(double value) {
        return new Vector3(x * value, y * value, z * value);
    }

    public double dot(Vector3 other) {
        return x * other.x + y * other.y + z * other.z;
    }

    public double length() {
        return Math.sqrt(dot(this));
    }

    public Vector3 normalize() {
        double length = length();
        return new Vector3(x / length, y / length, z / length);
    }
}

class Ray {
    public Vector3 origin;
    public Vector3 direction;

    public Ray(Vector3 origin, Vector3 direction) {
        this.origin = origin;
        this.direction = direction;
    }
}

class Sphere {
    public Vector3 center;
    public double radius;
    public Color color;

    public Sphere(Vector3 center, double radius, Color color) {
        this.center = center;
        this.radius = radius;
        this.color = color;
    }

    public double intersectRay(Ray ray) {
        // Ray-sphere intersection can result in a quadratic equation of form at^2 + bt + c = 0
        // Solve this equation to find t, the distance from the ray origin to the intersection point
        Vector3 oc = ray.origin.subtract(center);
        double a = ray.direction.dot(ray.direction);
        double b = 2.0 * oc.dot(ray.direction);
        double c = oc.dot(oc) - radius * radius;
        double discriminant = b * b - 4 * a * c;

        if (discriminant < 0) {
            return -1; // No intersection
        } else {
            return (-b - Math.sqrt(discriminant)) / (2.0 * a);
        }
    }
}

class Plane {
    public Vector3 normal;
    public double offset;
    public Color color;

    public Plane(Vector3 normal, double offset, Color color) {
        this.normal = normal;
        this.offset = offset;
        this.color = color;
    }

    public double intersectRay(Ray ray) {
        double denom = normal.dot(ray.direction);
        if (denom > 1e-6) {
            Vector3 p0l0 = normal.scale(offset).subtract(ray.origin);
            double t = p0l0.dot(normal) / denom;
            return (t >= 0) ? t : -1;
        } else {
            return -1; // No intersection or parallel
        }
    }
}
