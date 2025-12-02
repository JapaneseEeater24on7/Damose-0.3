# 🚌 Rome Bus Tracker

A real-time bus tracking application for Rome's public transit system, built with Java Swing and GTFS-RT data.

![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)
![Maven](https://img.shields.io/badge/Maven-3.x-C71A36?style=flat-square&logo=apachemaven)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

<p align="center">
  <img src="src/main/resources/sprites/bus.png" alt="Bus Icon" width="120"/>
</p>

## ✨ Features

- **Real-Time Bus Tracking** — Live bus positions on an interactive map using GTFS-RT feeds from Roma Mobilità
- **Arrival Predictions** — View upcoming arrivals at any stop with real-time delay/early status
- **Interactive Map** — Pan, zoom, and click on stops to see arrival information
- **Stop & Line Search** — Find stops by name or search for specific bus lines
- **Offline Mode** — Fallback to static schedule data when real-time feeds are unavailable
- **Modern Dark UI** — Sleek interface powered by FlatLaf with smooth fade animations

## 📸 How It Works

1. **Map View** — The main window displays an interactive map of Rome centered on the city
2. **Bus Icons** — Real-time bus positions are displayed as markers on the map
3. **Stop Markers** — Click on any bus stop to see upcoming arrivals
4. **Floating Panel** — Arrivals appear in a tooltip-style panel showing:
   - 🔴 Red dot = Bus is delayed
   - 🟢 Green dot = Bus is on time or early
   - ⚪ White dot = Static schedule (no real-time data)
5. **Search** — Use the search button (🔍) to find stops or lines

## 🛠️ Tech Stack

| Technology | Purpose |
|------------|---------|
| **Java 17** | Core language |
| **Swing** | GUI framework |
| **JXMapViewer2** | Interactive map rendering |
| **GTFS-RT Bindings** | Real-time transit data parsing |
| **Protocol Buffers** | Binary data serialization |
| **Apache Commons CSV** | GTFS static file parsing |
| **FlatLaf** | Modern look-and-feel theme |
| **Gson** | JSON processing |

## 📦 Installation

### Prerequisites

- Java 17 or higher
- Maven 3.x

### Build & Run

```bash
# Clone the repository
git clone https://github.com/yourusername/rome-bus-tracker.git
cd rome-bus-tracker

# Build the project
mvn clean compile

# Run the application
mvn exec:java
```

Or run directly:

```bash
mvn exec:java -Dexec.mainClass="project_starter.RealTimeBusTrackerApp"
```

## 📁 Project Structure

```
src/main/
├── java/project_starter/
│   ├── RealTimeBusTrackerApp.java    # Application entry point
│   ├── controller/
│   │   ├── RealTimeBusTrackerController.java  # Main controller
│   │   ├── ArrivalService.java       # Arrival time calculations
│   │   └── GestoreRealTime.java      # GTFS-RT feed management
│   ├── datas/
│   │   ├── StopsLoader.java          # GTFS stops.txt parser
│   │   ├── TripsLoader.java          # GTFS trips.txt parser
│   │   ├── StopTimesLoader.java      # GTFS stop_times.txt parser
│   │   ├── CalendarLoader.java       # Service calendar parsing
│   │   ├── TripMatcher.java          # Trip ID matching logic
│   │   └── ...                       # Other data utilities
│   ├── model/
│   │   ├── GTFSFetcher.java          # GTFS-RT feed parsing
│   │   ├── VehiclePosition.java      # Bus position model
│   │   ├── StaticSimulator.java      # Offline mode simulation
│   │   └── ...                       # Other models
│   ├── view/
│   │   ├── RealTimeBusTrackerView.java  # Main window
│   │   ├── FloatingArrivalPanel.java    # Arrival tooltip UI
│   │   ├── StopSearchPanel.java         # Search sidebar
│   │   ├── MapFactory.java              # Map configuration
│   │   └── ...                          # Other UI components
│   └── render/
│       ├── BusWaypointRenderer.java  # Bus marker rendering
│       └── StopWaypointRenderer.java # Stop marker rendering
└── resources/
    ├── gtfs_static/                  # Static GTFS data files
    │   ├── stops.txt
    │   ├── trips.txt
    │   ├── stop_times.txt
    │   ├── routes.txt
    │   └── ...
    └── sprites/                      # UI icons
        ├── bus.png
        ├── stop.png
        └── lente.png
```

## 🌐 Data Sources

This application uses GTFS and GTFS-RT data from [Roma Mobilità](https://romamobilita.it/):

| Feed | URL |
|------|-----|
| Vehicle Positions | `https://romamobilita.it/sites/default/files/rome_rtgtfs_vehicle_positions_feed.pb` |
| Trip Updates | `https://romamobilita.it/sites/default/files/rome_rtgtfs_trip_updates_feed.pb` |

Static GTFS data is bundled in `src/main/resources/gtfs_static/`.

## ⚙️ Configuration

The application automatically:
- Fetches real-time data every **30 seconds**
- Updates the map overlay every **15 seconds**
- Falls back to offline mode if real-time feeds are unavailable

## 🔧 Development

### Adding New Features

The codebase follows an MVC-like architecture:

- **Model** (`model/`) — Data structures and GTFS parsing
- **View** (`view/`) — Swing UI components
- **Controller** (`controller/`) — Business logic and coordination

### Key Classes

| Class | Description |
|-------|-------------|
| `RealTimeBusTrackerController` | Main orchestrator, initializes data and updates |
| `ArrivalService` | Computes arrival times, merges static + real-time data |
| `GestoreRealTime` | Manages GTFS-RT feed fetching and caching |
| `GTFSFetcher` | Parses Protocol Buffer feeds into Java objects |
| `RealTimeBusTrackerView` | Main Swing window with map and overlays |

## 📄 License

This project is open source. Feel free to use, modify, and distribute.

## 🤝 Contributing

Contributions are welcome! Feel free to:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 🙏 Acknowledgments

- [Roma Mobilità](https://romamobilita.it/) for providing open transit data
- [JXMapViewer2](https://github.com/msteiger/jxmapviewer2) for the excellent map library
- [FlatLaf](https://www.formdev.com/flatlaf/) for the modern Swing look-and-feel
- [MobilityData](https://gtfs.mobilitydata.org/) for GTFS-RT specifications

---

<p align="center">
  Made with ☕ and 🚌 in Rome
</p>
