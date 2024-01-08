package de.mrjulsen.crn.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import com.simibubi.create.content.trains.entity.Train;
import de.mrjulsen.crn.util.TrainUtils;

public class GlobalTrainData {

    private final Collection<Train> trains;
    private final Collection<String> stations;
    private final Map<String, Collection<DeparturePrediction>> aliasPredictions = new HashMap<>();
    private final Map<UUID, Collection<DeparturePrediction>> trainPredictions = new HashMap<>();
    private final long updateTime;

    private static GlobalTrainData instance = null;

    private GlobalTrainData(long updateTime) {
        trains = TrainUtils.getAllTrains();
        stations = TrainUtils.getAllStations().parallelStream().map(x -> x.name).toList();
        TrainUtils.getMappedDeparturePredictions(aliasPredictions, trainPredictions);
        this.updateTime = updateTime;
    }

    public static GlobalTrainData makeSnapshot(long updateTime) {
        return instance = new GlobalTrainData(updateTime);
    }

    public static GlobalTrainData getInstance() {
        return instance;
    }

    public long getUpdateTime() {
        return updateTime;
    }


    public boolean stationHasDepartingTrains(TrainStationAlias alias) {
        return aliasPredictions.containsKey(alias.getAliasName().get());
    }   
    
    public final Collection<Train> getAllTrains() {
        return trains;
    }

    public final Collection<String> getAllStations() {
        return stations;
    }

    public Collection<DeparturePrediction> getPredictionsOfTrain(Train train) {
        return trainPredictions.get(train.id);
    }

    public Collection<DeparturePrediction> getPredictionsOfTrainChronologically(Train train) {
        return getPredictionsOfTrain(train).parallelStream().sorted(Comparator.comparingInt(x -> x.getTicks())).toList();
    }

    public Optional<DeparturePrediction> getNextStop(Train train) {
        return getPredictionsOfTrainChronologically(train).stream().findFirst();
    }

    
    public boolean trainStopsAt(Train train, TrainStationAlias station) {
        return getPredictionsOfTrain(train).parallelStream().anyMatch(x -> x.getNextStop().equals(station));
    }


    public Collection<DeparturePrediction> getTrainStopDataAt(Train train, TrainStationAlias station) {
        Collection<DeparturePrediction> predictions = getPredictionsOfTrain(train);
        return predictions.parallelStream().filter(x -> x.getNextStop().equals(station)).toList();
    }

    public Collection<DeparturePrediction> getSortedTrainStopDataAt(Train train, TrainStationAlias station) {
        return getTrainStopDataAt(train, station).parallelStream().sorted(Comparator.comparingInt(x -> x.getTicks())).toList();
    }

    public Optional<DeparturePrediction> getNextTrainStopDataAt(Train train, TrainStationAlias station) {   
        return getTrainStopDataAt(train, station).stream().findFirst();
    }

    public Collection<TrainStop> getAllStops(Train train) {
        return getPredictionsOfTrain(train).parallelStream().map(x -> new TrainStop(x.getNextStop(), x)).toList();
    }

    public Collection<TrainStop> getAllStopsSorted(Train train) {
        return getAllStops(train).parallelStream().sorted(Comparator.comparingInt(x -> x.getPrediction().getTicks())).toList();
    }

    public SimpleTrainSchedule getTrainSimpleSchedule(Train train) {
        return new SimpleTrainSchedule(train);
    }

    public List<TrainStop> getAllStopoversOfTrainSortedNew(Train train, TrainStationAlias start, TrainStationAlias end, boolean includeStartEnd, boolean correctStart) {
        Collection<TrainStop> stops = getAllStopsFrom(train, start, false).getAllStops();
        
        if (stops.parallelStream().noneMatch(x -> x.isStationAlias(start) || x.isStationAlias(end))) {
            return new ArrayList<>();
        }

        DeparturePrediction startPrediction = null;
        DeparturePrediction endPrediction = null;
        int ticksStart = -1;
        int ticksStop = -1;

        Collection<DeparturePrediction> startStopDatas = getSortedTrainStopDataAt(train, start);
        Collection<DeparturePrediction> endStopDatas = getSortedTrainStopDataAt(train, end);

        Optional<DeparturePrediction> firstStartData = startStopDatas.stream().findFirst();

        if (firstStartData.isPresent()) {
            ticksStart = firstStartData.get().getTicks();
        }
        final int ftmpTicksStart = ticksStart;

        if (endStopDatas != null && endStopDatas.size() > 0) {
            // Die erste Endstation, die nach der ersten Startstation kommt.
            Optional<DeparturePrediction> endStopData = endStopDatas.stream()
                .filter(x -> x.getTicks() >= ftmpTicksStart)
                .findFirst();
                
            if (endStopData.isPresent()) {
                ticksStop = (endPrediction = endStopData.get()).getTicks();
            } else {
                endStopData = endStopDatas.stream().findFirst();
                if (endStopData.isPresent()) {
                    ticksStop = (endPrediction = endStopData.get()).getTicks();
                }
            }
        }
        final int fTicksStop = ticksStop;

        if (startStopDatas != null && startStopDatas.size() > 0) {
            DeparturePrediction startStopData = correctStart ?
                startStopDatas.stream()
                    .filter(x -> x.getTicks() <= fTicksStop)
                    .reduce((a, b) -> b).orElse(null)
            : startPrediction;
                
            if (startStopData != null) {
                ticksStart = (startPrediction = startStopData).getTicks();
            } else {
                startStopData = startStopDatas.stream().reduce((a, b) -> b).orElse(null);
                if (startStopData != null) {
                    ticksStart = (startPrediction = startStopData).getTicks();
                }
            }
        }

        if (correctStart) {            
            
        }
        final int fTicksStart = ticksStart;

        List<TrainStop> filteredStops = new ArrayList<>();
        if (fTicksStart <= fTicksStop) {
            filteredStops.addAll(stops.stream().filter(x -> (x.getPrediction().getTicks() > fTicksStart && x.getPrediction().getTicks() < fTicksStop)).toList());
        } else {
            filteredStops.addAll(stops.stream().filter(x -> (x.getPrediction().getTicks() < fTicksStop || x.getPrediction().getTicks() > fTicksStart)).map(x -> {
                if (x.getPrediction().getTicks() < fTicksStop) {
                    return new TrainStop(x.getStationAlias(), DeparturePrediction.withNextCycleTicks(x.getPrediction()));
                }
                return x;
            }).toList());
        }

        if (includeStartEnd) {
            filteredStops.add(new TrainStop(end, fTicksStop < fTicksStart ? DeparturePrediction.withNextCycleTicks(endPrediction) : endPrediction));
            filteredStops.add(0, new TrainStop(start, startPrediction));
        }

        return filteredStops;
    }

    public SimpleTrainSchedule getAllStopsFrom(Train train, TrainStationAlias alias, boolean preventDuplicates) {
        List<TrainStop> newList = new ArrayList<>();
        int idx = 0;
        for (TrainStop stop : getAllStopsSorted(train)) {            
            if (preventDuplicates && newList.contains(stop)) {
                continue;
            }

            if (stop.getStationAlias().equals(alias)) {
                idx = 0;
            }

            newList.add(idx, stop);
            idx++;
        }
        return SimpleTrainSchedule.of(newList);
    }


    public SimpleTrainSchedule getAllStopsDirectional(Train train, TrainStationAlias alias) {
        List<TrainStop> newList = new ArrayList<>();
        for (TrainStop stop : getAllStopsFrom(train, alias, false).getAllStops()) {  
            if (newList.contains(stop)) {
                break;
            }

            newList.add(stop);
        }
        return SimpleTrainSchedule.of(newList);
    }

    
    public SimpleTrainSchedule getDirectionalSchedule(Train train) {
        List<TrainStop> newList = new ArrayList<>();
        boolean isRepeating = false;
        for (TrainStop stop : getAllStopsSorted(train)) {            
            if (newList.contains(stop)) {
                isRepeating = true;
                continue;
            }

            if (isRepeating) {
                newList.add(0, stop);
            } else {
                newList.add(stop);
            }
        }
        return SimpleTrainSchedule.of(newList);
    }

    public Collection<DeparturePrediction> getDepartingTrainsAt(TrainStationAlias station) {
        return aliasPredictions.getOrDefault(station.getAliasName().get(), Collections.emptyList()).parallelStream().sorted(Comparator.comparingInt(x -> x.getTicks())).toList();
    }

    public Optional<DeparturePrediction> getNextDepartingTrainAt(TrainStationAlias station) {
        Collection<DeparturePrediction> predictions = getDepartingTrainsAt(station);

        if (predictions == null || predictions.isEmpty()) {
            return Optional.empty();
        }

        return predictions.stream().findFirst();
    }
}
