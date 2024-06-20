package net.crashcraft.crashclaim.claimobjects;

import java.util.UUID;

public class SubClaim extends BaseClaim {
    private final Claim parent;
    private int UpperCornerY = -500;
    private int LowerCornerY = -500;

    //both -500 as default because they need a numerical value so that the unique restraint works in the db

    public SubClaim(Claim parent, int id, int upperCornerX, int upperCornerZ, int lowerCornerX, int lowerCornerZ, UUID world, PermissionGroup perms) {
        super(id, upperCornerX, upperCornerZ, lowerCornerX, lowerCornerZ, world, perms);
        this.parent = parent;
    }

    public SubClaim(Claim parent, int id, int upperCornerX, int upperCornerZ, int lowerCornerX, int lowerCornerZ, int upperCornerY, int lowerCornerY, UUID world, PermissionGroup perms) {
        super(id, upperCornerX, upperCornerZ, lowerCornerX, lowerCornerZ, world, perms);
        UpperCornerY = Math.max(lowerCornerY, upperCornerY);
        LowerCornerY = Math.min(lowerCornerY, upperCornerY);

        this.parent = parent;
    }

    @Override
    public void setToSave(boolean toSave) {
        if (parent == null){
            // Needed for json setting this should never happen after load
            return;
        }
        parent.setToSave(true);
    }

    @Override
    public boolean isToSave() {
        return parent.isToSave();
    }

    @Override
    public UUID getWorld(){
        return parent.getWorld();
    }

    public Claim getParent() {
        return parent;
    }

    public int getMaxY(){
        return UpperCornerY;
    }
    public int getMinY(){
        return LowerCornerY;
    }

    public void SetMaxCornerY(int NewMaxY){
        UpperCornerY = NewMaxY;
    }

    public void SetMinCornerY(int NewMinY){
        LowerCornerY = NewMinY;
    }

    public boolean IsVertical(){
        return (UpperCornerY != -500 && LowerCornerY != -500);
    }

}
