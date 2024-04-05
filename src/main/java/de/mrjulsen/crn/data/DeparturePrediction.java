package de.mrjulsen.crn.data;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import com.simibubi.create.content.trains.display.GlobalTrainDisplayData.TrainDeparturePrediction;
import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.data.TrainStationAlias.StationInfo;
import de.mrjulsen.crn.event.listeners.TrainListener;
import net.minecraft.nbt.CompoundTag;

public class DeparturePrediction {

    private Train train;
    private int ticks;
    private String scheduleTitle;
    private String nextStop;
    private StationInfo info;

    // Special data
    private TrainExitSide exit = TrainExitSide.UNKNOWN;

    private int cycle;

    public DeparturePrediction(Train train, int ticks, String scheduleTitle, String nextStop, int cycle, StationInfo info) {
        this.train = train;
        this.ticks = ticks;
        this.scheduleTitle = scheduleTitle;
        this.nextStop = nextStop;
        this.cycle = cycle;
        this.info = info;
    }
    
    public DeparturePrediction(TrainDeparturePrediction prediction) {
        this(prediction.train, prediction.ticks, prediction.scheduleTitle.getString(), prediction.destination, 0, GlobalSettingsManager.getInstance().getSettingsData().getAliasFor(prediction.destination).getInfoForStation(prediction.destination));
    }

    public DeparturePrediction copy() {
        return new DeparturePrediction(getTrain(), getTicks(), getScheduleTitle(), getStationName(), getCycle(), getInfo());
    }

    public static DeparturePrediction withNextCycleTicks(DeparturePrediction current) {
        int cycle = current.getCycle() + 1;
        return new DeparturePrediction(current.getTrain(), (getTrainCycleDuration(current.getTrain()) * cycle) + current.getTicks(), current.getScheduleTitle(), current.getStationName(), cycle, current.getInfo());
    }

    public static DeparturePrediction withCycleTicks(DeparturePrediction current, int cycles) {
        return new DeparturePrediction(current.getTrain(), (getTrainCycleDuration(current.getTrain()) * cycles) + current.getTicks(), current.getScheduleTitle(), current.getStationName(), cycles, current.getInfo());
    }

    public Train getTrain() {
        return train;
    }

    public int getTicks() {
        return ticks;
    }

    public String getScheduleTitle() {
        return scheduleTitle;
    }

    public String getStationName() {
        return nextStop;
    }

    public StationInfo getInfo() {
        return info;
    }

    public TrainStationAlias getNextStop() {
        return GlobalSettingsManager.getInstance().getSettingsData().getAliasFor(nextStop);
    }

    public int getCycle() {
        return cycle;
    }

    public int getTrainCycleDuration() {
        return getTrainCycleDuration(getTrain());
    }

    public static int getTrainCycleDuration(Train train) {
        return TrainListener.getInstance().getApproximatedTrainDuration(train);
    }

    public TrainExitSide getExitSide() {
        return this.exit;
    }

    public void setExit(TrainExitSide exit) {
        this.exit = exit;
    }

    public SimpleDeparturePrediction simplify() {
        return new SimpleDeparturePrediction(getNextStop().getAliasName().get(), nextStop, getTicks(), getScheduleTitle(), getTrain().name.getString(), getTrain().id, getInfo(), getExitSide());
    }


    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DeparturePrediction other) {
            return getTrain().id == other.getTrain().id && getTicks() == other.getTicks() && getScheduleTitle().equals(other.getScheduleTitle()) && getStationName().equals(other.getStationName());
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 19 * Objects.hash(getTrain().id, getTicks(), getScheduleTitle(), getStationName());
    }

    public boolean similar(DeparturePrediction other) {
        return getTicks() == other.getTicks() && getStationName().equals(other.getStationName());
    }

    @Override
    public String toString() {
        return String.format("%s, Next stop: %s in %st", getTrain().name.getString(), getNextStop().getAliasName(), getTicks());
    }

    public static enum TrainExitSide {
        UNKNOWN((byte)0),
        RIGHT((byte)1),
        LEFT((byte)-1);

        private byte side;

        TrainExitSide(byte side) {
            this.side = side;
        }

        public byte getAsByte() {
            return side;
        }

        public static TrainExitSide getFromByte(byte side) {
            return Arrays.stream(values()).filter(x -> x.getAsByte() == side).findFirst().orElse(UNKNOWN);
        }

        public TrainExitSide getOpposite() {
            return getFromByte((byte)-getAsByte());
        }
    }

    public static record SimpleDeparturePrediction(String stationTagName, String stationName, int departureTicks, String scheduleTitle, String trainName, UUID trainId, StationInfo stationInfo, TrainExitSide exitSide) {

        private static final String NBT_STATION = "station";
        private static final String NBT_STATION_NAME = "stationName";
        private static final String NBT_TICKS = "ticks";
        private static final String NBT_SCHEDULE_TITLE = "title";
        private static final String NBT_TRAIN_NAME = "tname";
        private static final String NBT_ID = "id";
        private static final String NBT_EXIT_SIDE = "exit";

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putString(NBT_STATION, stationTagName);
            nbt.putString(NBT_STATION_NAME, stationName);
            nbt.putInt(NBT_TICKS, departureTicks);
            nbt.putString(NBT_SCHEDULE_TITLE, scheduleTitle);
            nbt.putString(NBT_TRAIN_NAME, trainName);
            nbt.putUUID(NBT_ID, trainId);
            stationInfo().writeNbt(nbt);
            nbt.putByte(NBT_EXIT_SIDE, exitSide.getAsByte());
            return nbt;
        }

        public static SimpleDeparturePrediction fromNbt(CompoundTag nbt) {
            return new SimpleDeparturePrediction(
                nbt.getString(NBT_STATION),
                nbt.getString(NBT_STATION_NAME),
                nbt.getInt(NBT_TICKS), 
                nbt.getString(NBT_SCHEDULE_TITLE),
                nbt.getString(NBT_TRAIN_NAME),
                nbt.getUUID(NBT_ID),
                StationInfo.fromNbt(nbt),
                TrainExitSide.getFromByte(nbt.getByte(NBT_EXIT_SIDE))
            );
        }
    }
}
