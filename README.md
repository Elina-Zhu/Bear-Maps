# Bear Maps

Bear Maps is a web mapping application similar to Google Maps. This project showcases a server backend capable of rendering map tiles and providing route-finding functionality between arbitrary points on the map.

## Features

| Feature              | Description                                                                                                              |
|----------------------|--------------------------------------------------------------------------------------------------------------------------|
| Map Rastering        | Dynamically renders map tiles based on the user's view, adjusting for zoom level and viewport.                            |
| Autocomplete Search  | Suggests locations to the user as they type, utilizing a Trie for efficient prefix-matching.                              |
| Route Finding        | Employs the A* algorithm to find and visualize the shortest path between two points on the map.                           |
| Location Search      | Allows users to search for locations, providing backend support for translating location names to map coordinates.       |
| Turn-by-Turn Navigation | Generates and displays turn-by-turn directions as part of the route-finding feature.                                      |
| API Integration      | Interfaces with location data APIs to fetch and incorporate a wide range of location information into the map.            |

## Getting Started

To get a local copy up and running, follow these simple steps:

1. Clone this repo to your local machine using `https://github.com/your-username/Bear-Maps.git`.
2. Compile the project using Maven: `mvn compile`.
3. Run the server: `mvn exec:java -Dexec.mainClass="Main"`.

## Usage

Once the server is running, navigate to `localhost:4567` in your web browser to start using Bear Maps. 

## Built With

- Java
- Apache Maven
- JavaScript
- HTML/CSS

## Acknowledgments

- This project is a part of the coursework for Berkeley's CS61B: Data Structures. The structure and test suite were provided by the course staff at Berkeley.
- The tile images and map feature data was downloaded from OpenStreetMap.
