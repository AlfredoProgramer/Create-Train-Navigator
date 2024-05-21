package de.mrjulsen.crn.network.packets.cts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.simibubi.create.content.trains.entity.Train;

import de.mrjulsen.crn.Constants;
import de.mrjulsen.crn.CreateRailwaysNavigator;
import de.mrjulsen.crn.client.lang.ELanguage;
import de.mrjulsen.crn.data.DeparturePrediction.TrainExitSide;
import de.mrjulsen.crn.data.DeparturePrediction.SimpleDeparturePrediction;
import de.mrjulsen.crn.data.GlobalSettingsManager;
import de.mrjulsen.crn.data.SimpleTrainSchedule;
import de.mrjulsen.crn.data.TrainStop;
import de.mrjulsen.crn.network.packets.stc.TrainDataResponsePacket;
import de.mrjulsen.crn.util.TrainUtils;
import de.mrjulsen.mcdragonlib.data.Cache;
import de.mrjulsen.mcdragonlib.net.IPacketBase;
import dev.architectury.networking.NetworkManager.PacketContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public class TrainDataRequestPacket implements IPacketBase<TrainDataRequestPacket> {

    public long requestId;
    public UUID trainId;
    public boolean getPredictions;

    public TrainDataRequestPacket() { }
    
    public TrainDataRequestPacket(long requestId, UUID trainId, boolean getPredictions) {
        this.requestId = requestId;
        this.trainId = trainId;
        this.getPredictions = getPredictions;
    }

    @Override
    public void encode(TrainDataRequestPacket packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.requestId);
        buffer.writeUUID(packet.trainId);
        buffer.writeBoolean(packet.getPredictions);
    }

    @Override
    public TrainDataRequestPacket decode(FriendlyByteBuf buffer) {
        long requestId = buffer.readLong();
        UUID trainId = buffer.readUUID();
        boolean getPredictions = buffer.readBoolean();

        return new TrainDataRequestPacket(requestId, trainId, getPredictions);
    }

    @Override
    public void handle(TrainDataRequestPacket packet, Supplier<PacketContext> contextSupplier) {
        contextSupplier.get().queue(() -> {
            final Level level = contextSupplier.get().getPlayer().getLevel();
            final long updateTime = level.getDayTime();
            Train train = TrainUtils.getTrain(packet.trainId);
            List<SimpleDeparturePrediction> departurePredictions = new ArrayList<>();
            if (packet.getPredictions && train != null) {
                Collection<TrainStop> stops = new ArrayList<>(TrainUtils.getTrainStopsSorted(packet.trainId, level).stream().filter(x -> !GlobalSettingsManager.getInstance().getSettingsData().isBlacklisted(x.getPrediction().getStationName())).toList());
                if (stops != null) {
                    departurePredictions.addAll(SimpleTrainSchedule.of(stops).makeScheduleUntilNextRepeat().getAllStops().stream().map(x -> x.getPrediction().simplify()).toList());
                }
            }

            CreateRailwaysNavigator.net().CHANNEL.sendToPlayer((ServerPlayer)contextSupplier.get().getPlayer(), new TrainDataResponsePacket(packet.requestId, new TrainData(
                packet.trainId,
                train.name.getString(),
                departurePredictions,
                train.speed,
                train.navigation.ticksWaitingForSignal,
                train.currentlyBackwards,
                true
            ), updateTime));
        });
    }
    
    public static class TrainData {
        private static final String NBT_TRAIN_ID = "Id";
        private static final String NBT_SPEED = "Speed";
        private static final String NBT_WAITING_FOR_SIGNAL = "WaitingForSignal";
        private static final String NBT_NAME = "Name";
        private static final String NBT_PREDICTIONS = "Predictions";
        private static final String NBT_TRAIN_DIRECTION = "Direction";
        private static final String NBT_ON_TRAIN = "OnTrain";

        private final UUID trainId;
        private final String trainName;
        private final List<SimpleDeparturePrediction> predictions;
        private final double speed;
        private final int ticksWaitingForSignal;
        private final boolean oppositeDirection;

        private final boolean onTrain;
        
        private final Cache<List<SimpleDeparturePrediction>> stopovers = new Cache<>(() -> {
            List<SimpleDeparturePrediction> s = new ArrayList<>();
            if (predictions().size() >= 2) {
                for (int i = 1; i < predictions().size() - 1; i++) {
                    s.add(predictions().get(i));
                }
            }
            return s;
        });

        public TrainData(UUID trainId, String trainName, List<SimpleDeparturePrediction> predictions, double speed, int ticksWaitingForSignal, boolean oppositeDirection, boolean onTrain) {
            this.trainId = trainId;
            this.trainName = trainName;
            this.predictions = predictions;
            this.speed = speed;
            this.ticksWaitingForSignal = ticksWaitingForSignal;
            this.oppositeDirection = oppositeDirection;
            this.onTrain = onTrain;
        }

        public UUID trainId() {
            return trainId;
        }

        public String trainName() {
            return trainName;
        }

        public List<SimpleDeparturePrediction> predictions() {
            return predictions;
        }

        public double speed() {
            return speed;
        }

        public int ticksWaitingForSignal() {
            return ticksWaitingForSignal;
        }

        public List<SimpleDeparturePrediction> stopovers() {
            return stopovers.get();
        }

        public boolean isOppositeDirection() {
            return oppositeDirection;
        }

        public CompoundTag toNbt() {
            CompoundTag nbt = new CompoundTag();
            nbt.putUUID(NBT_TRAIN_ID, trainId);
            nbt.putDouble(NBT_SPEED, speed);
            nbt.putInt(NBT_WAITING_FOR_SIGNAL, ticksWaitingForSignal);
            nbt.putString(NBT_NAME, trainName);
            nbt.putBoolean(NBT_TRAIN_DIRECTION, oppositeDirection);
            nbt.putBoolean(NBT_ON_TRAIN, onTrain);

            ListTag list = new ListTag();
            list.addAll(predictions().stream().map(x -> x.toNbt()).toList());
            nbt.put(NBT_PREDICTIONS, list);
            return nbt;
        }

        public static TrainData fromNbt(CompoundTag nbt) {
            return new TrainData(
                nbt.getUUID(NBT_TRAIN_ID),
                nbt.getString(NBT_NAME),
                nbt.getList(NBT_PREDICTIONS, Tag.TAG_COMPOUND).stream().map(x -> SimpleDeparturePrediction.fromNbt(((CompoundTag)x))).toList(),
                nbt.getDouble(NBT_SPEED),
                nbt.getInt(NBT_WAITING_FOR_SIGNAL),
                nbt.getBoolean(NBT_TRAIN_DIRECTION),
                nbt.getBoolean(NBT_ON_TRAIN)
            );
        }

        public Optional<SimpleDeparturePrediction> getNextStop() {
            return predictions().size() > 0 ? Optional.of(predictions().get(0)) : Optional.empty();
        }

        public Optional<SimpleDeparturePrediction> getLastStop() {
            return predictions().size() > 0 ? Optional.of(predictions().get(predictions().size() - 1)) : Optional.empty();
        }

        public static TrainData empty(boolean onTrain) {
            MutableComponent text = onTrain ? ELanguage.translate("block.createrailwaysnavigator.advanced_display.ber.shunting_trip") : ELanguage.translate("block.createrailwaysnavigator.advanced_display.ber.not_in_service");
            return new TrainData(Constants.ZERO_UUID, "", List.of(new SimpleDeparturePrediction("", "", 0, text.getString(), "", Constants.ZERO_UUID, null, TrainExitSide.UNKNOWN)), 0, 0, false, onTrain);
        }

    }
}
