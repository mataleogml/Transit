# Transit - Your All-in-One Minecraft Transportation Management Solution

🚇 A comprehensive Minecraft plugin for managing public transportation systems including subways, buses, and railways.

[![GitHub release](https://img.shields.io/github/v/release/yourusername/Transit)](https://github.com/yourusername/Transit/releases)
[![License](https://img.shields.io/github/license/yourusername/Transit)](LICENSE)
[![Spigot](https://img.shields.io/badge/Spigot-1.20.4-yellow.svg)](https://www.spigotmc.org/resources/transit.12345)

## Features

### 🌟 Core Features
- **Multiple Transit Systems**
  - Subway/Metro systems
  - Bus networks
  - Railway lines
  - Expandable for custom systems

- **Advanced Fare Management**
  - Zone-based fares
  - Distance-based fares
  - Flat-rate fares
  - Interchange support between systems

- **Dynamic Route System**
  - Interactive route maps
  - Interchange stations
  - Real-time journey tracking
  - Custom route creation

- **Staff Management**
  - System-specific staff roles
  - Automated salary payments
  - Staff access controls
  - Performance tracking

### 💡 Smart Features
- **Real-time Journey Tracking**
  - Journey progress display
  - Automated fare calculation
  - Time-based maximum fare charging
  - Multi-system journey support

- **Interactive Gates**
  - Smart fare gates
  - Emergency exit support
  - Staff override capabilities
  - Status monitoring

## 📋 Requirements
- Server version: 1.20+
- Dependencies:
  - Vault
  - (Optional) PlaceholderAPI
  - (Optional) WorldGuard

## 🚀 Quick Start

### Installation
1. Download the latest release
2. Place in your server's `plugins` folder
3. Restart your server
4. Configure in `plugins/Transit/config.yml`

### Basic Setup

#### 1. Create a Transit System
```
/transit create Subway "Metro System" ZONE
```

#### 2. Add Stations
```
/station add Subway Central_Station
/station add Subway Downtown
```

#### 3. Create Routes
```
/route add Subway Yellow_Line Central_Station
/route add Subway Yellow_Line Downtown
```

#### 4. Setup Fare Gates
Place a sign with:
```
[Fare]
```
Follow the chat prompts to configure the gate.

## 📖 Detailed Configuration

### Transit Systems
```yaml
systems:
  Subway:
    name: "Metro Subway"
    fareType: ZONE
    maxFare: 8.00
    zones:
      rings:
        "1": 0  # Central zone
        "2": 1  # First ring
      groups:
        city_center: ["1A", "1B"]
        suburbs: ["2A", "2B"]
```

### Fare Rules
```yaml
rules:
  same_zone:
    from: "\\d+"
    to: "\\1"
    fare: 2.50
  adjacent:
    ringDifference: 1
    fare: 3.00
```

### Staff Configuration
```yaml
staff:
  minimumBalance: 5000.0
  paymentNotifications: true
  defaultPeriod: MONTHLY
```

## 🎮 Commands

### Transit System Management (`/transit`)
Base Permission: `transit.admin`

| Command | Description | Usage |
|---------|-------------|--------|
| `/transit create <id> <name> <faretype>` | Create a new transit system | `/transit create Subway "City Metro" ZONE` |
| `/transit info <system>` | View system information | `/transit info Subway` |
| `/transit config reload` | Reload configuration | `/transit config reload` |
| `/transit config save` | Save configuration | `/transit config save` |

### Station Management (`/station`)
Base Permission: `transit.admin` for management, `transit.tp` for teleporting

| Command | Description | Usage |
|---------|-------------|--------|
| `/station add <system> <name> [zone]` | Create a new station | `/station add Subway Central 1` |
| `/station remove <system> <name>` | Remove a station | `/station remove Subway Central` |
| `/station enable <system> <name>` | Enable a station | `/station enable Subway Central` |
| `/station disable <system> <name>` | Disable a station | `/station disable Subway Central` |
| `/station maintenance <system> <name>` | Set station maintenance mode | `/station maintenance Subway Central` |
| `/station tp <system> <name>` | Teleport to station | `/station tp Subway Central` |
| `/station list <system>` | List all stations | `/station list Subway` |

### Route Management (`/route`)
Base Permission: `transit.admin`

| Command | Description | Usage |
|---------|-------------|--------|
| `/route add <system> <name>` | Create a new route | `/route add Subway RedLine` |
| `/route show <route>` | Display route information | `/route show Subway_RedLine` |
| `/route addstation <route> <station>` | Add station to route | `/route addstation Subway_RedLine Central` |
| `/route reorder <route>` | Reorder stations in route | `/route reorder Subway_RedLine` |
| `/route remove <route>` | Remove a route | `/route remove Subway_RedLine` |

### Fare Management (`/fare`)
Base Permission: `transit.admin`

| Command | Description | Usage |
|---------|-------------|--------|
| `/fare refund <uuid>` | Refund a transaction | `/fare refund abc-123-def` |
| `/fare <system>` | Check system balance | `/fare Subway` |

### Staff Management (`/staff`)
Base Permission: `transit.staff.*`

| Command | Description | Usage |
|---------|-------------|--------|
| `/staff list <system>` | List all staff members | `/staff list Subway` |
| `/staff add <system> <player> <salary> [role] [period]` | Add staff member | `/staff add Subway steve 1000 CONDUCTOR MONTHLY` |
| `/staff remove <system> <player>` | Remove staff member | `/staff remove Subway steve` |
| `/staff role <system> <player> <role>` | Change staff role | `/staff role Subway steve SUPERVISOR` |
| `/staff shift <start/end> <system>` | Manage staff shifts | `/staff shift start Subway` |
| `/staff performance <player>` | View staff performance | `/staff performance steve` |
| `/staff salary <system> <player> <amount>` | Update staff salary | `/staff salary Subway steve 1200` |

### Statistics (`/stats`)
Base Permission: `transit.stats`

| Command | Description | Usage |
|---------|-------------|--------|
| `/stats system <systemId> [period]` | View system statistics | `/stats system Subway MONTHLY` |
| `/stats station <stationId> [period]` | View station statistics | `/stats station Subway_Central WEEKLY` |
| `/stats route <routeId> [period]` | View route statistics | `/stats route Subway_RedLine DAILY` |
| `/stats report <systemId> <startDate> <endDate>` | Generate detailed report | `/stats report Subway 2024-01-01 2024-01-31` |
| `/stats peaks <systemId>` | View peak usage times | `/stats peaks Subway` |
| `/stats export <systemId> <type>` | Export statistics | `/stats export Subway csv` |

## 🔑 Permission Nodes

### Administrative
- `transit.admin` - Full administrative access
- `transit.stats` - Access to statistics
- `transit.staff.*` - All staff management permissions
  - `transit.staff.add` - Add staff members
  - `transit.staff.remove` - Remove staff members
  - `transit.staff.list` - List staff members
  - `transit.staff.role` - Modify staff roles
  - `transit.staff.salary` - Modify staff salaries
  - `transit.staff.performance` - View performance stats

### General Use
- `transit.tp` - Teleport to stations
- `transit.use` - Use transit systems

## 📊 Statistics Periods
Available periods for statistical commands:
- `DAILY` - Current day
- `WEEKLY` - Current week
- `MONTHLY` - Current month
- `ALL_TIME` - All recorded data

## 🎭 Staff Roles
Available staff roles:
- `TRAINEE` - New staff members
- `CONDUCTOR` - Regular staff
- `SUPERVISOR` - Senior staff
- `MANAGER` - System managers

## 💰 Payment Periods
Available payment periods for staff:
- `DAILY` - Daily payments
- `WEEKLY` - Weekly payments
- `MONTHLY` - Monthly payments

## 🔧 API Integration

### Maven Dependency
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>transit</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

### Basic Usage
```kotlin
val transitAPI = server.pluginManager.getPlugin("Transit") as? TransitPlugin

// Get station info
val station = transitAPI?.getStation("Subway", "Central_Station")

// Check staff status
val isStaff = transitAPI?.isStaffMember(player, "Subway")
```

### Event Listening
```kotlin
@EventHandler
fun onTransitGateEntry(event: TransitGateEntryEvent) {
    val player = event.player
    val station = event.station
    // Custom handling
}
```

## 🎯 Common Use Cases

### Setting Up a Metro System
1. Create the system:
   ```
   /transit create Subway "City Metro" ZONE
   ```

2. Define stations:
   ```
   /station add Subway Central
   /station add Subway North
   /station add Subway South
   ```

3. Create routes:
   ```
   /route add Subway Red_Line Central
   /route add Subway Red_Line North
   /route add Subway Red_Line South
   ```

4. Place fare gates and configure zones

### Managing Staff
1. Add staff member:
   ```
   /transit staff add Subway steve
   ```

2. Set salary:
   ```
   /transit staff salary Subway steve 1000 MONTHLY
   ```

## 📊 Statistics and Monitoring

- View system revenue:
  ```
  /transit stat Subway
  ```

- Check route usage:
  ```
  /transit stat Subway Red_Line
  ```

- Monitor station activity:
  ```
  /transit stat Subway Central
  ```

## 🔍 Troubleshooting

### Common Issues
1. **Gates not working**
   - Check station status
   - Verify sign format
   - Confirm permissions

2. **Fares not charging**
   - Check Vault connection
   - Verify fare configuration
   - Check system status

### Debug Mode
Enable debug mode in config.yml:
```yaml
settings:
  debug: true
```

## 🤝 Contributing
1. Fork the repository
2. Create your feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📜 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🌟 Support
- GitHub Issues: [Report a bug](https://github.com/yourusername/Transit/issues)
- Discord: [Join our community](https://discord.gg/yourdiscord)
- Wiki: [Detailed documentation](https://github.com/yourusername/Transit/wiki)

## ✨ Acknowledgments
- Contributors
- Spigot community
- Used libraries and dependencies

---

Made by Gabriel