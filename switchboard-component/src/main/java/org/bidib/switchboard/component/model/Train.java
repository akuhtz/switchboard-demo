package org.bidib.switchboard.component.model;

import java.awt.Image;
import java.util.Objects;

import javax.swing.ImageIcon;

/**
 * Model for a train identified by a unique id, an optional DCC address, a name and an image.
 */
public class Train {

    private final String id;

    private Integer address;

    private String name;

    private transient Image image;

    /** Base64-encoded image for JSON serialization. */
    private String imageBase64;

    public Train(String id, String name, Image image) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.image = image;
    }

    public String getId() {
        return id;
    }

    public Integer getAddress() {
        return address;
    }

    public void setAddress(Integer address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    /**
     * Returns the image as an ImageIcon scaled to the given size, or a placeholder
     * if no image is set.
     */
    public ImageIcon getScaledIcon(int size) {
        if (image != null) {
            return new ImageIcon(image.getScaledInstance(size, size, Image.SCALE_SMOOTH));
        }
        // Placeholder: a simple colored square
        var placeholder = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = placeholder.createGraphics();
        g.setColor(new java.awt.Color(100, 100, 120));
        g.fillRect(0, 0, size, size);
        g.dispose();
        return new ImageIcon(placeholder);
    }

    @Override
    public String toString() {
        String addr = address != null ? String.valueOf(address) : "?";
        return addr + " - " + name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Train t)) return false;
        return id.equals(t.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
