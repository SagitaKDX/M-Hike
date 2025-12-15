import React, { useState } from 'react';
import { View, StyleSheet, FlatList } from 'react-native';
import {
  TextInput,
  Card,
  Title,
  Text,
  Button,
  RadioButton,
  Divider,
} from 'react-native-paper';
import { searchHikesByName, advancedSearchHikes } from '../database/DatabaseHelper';

const SearchHikeScreen = ({ navigation }) => {
  const [searchMode, setSearchMode] = useState('simple');
  const [searchTerm, setSearchTerm] = useState('');
  const [advancedCriteria, setAdvancedCriteria] = useState({
    name: '',
    location: '',
    date: '',
    length: '',
  });
  const [results, setResults] = useState([]);
  const [hasSearched, setHasSearched] = useState(false);

  const handleSimpleSearch = async () => {
    if (searchTerm.trim()) {
      const fetchedHikes = await searchHikesByName(searchTerm);
      setResults(fetchedHikes);
      setHasSearched(true);
    }
  };

  const handleAdvancedSearch = async () => {
    const criteria = {};
    if (advancedCriteria.name) criteria.name = advancedCriteria.name;
    if (advancedCriteria.location) criteria.location = advancedCriteria.location;
    if (advancedCriteria.date) criteria.date = advancedCriteria.date;
    if (advancedCriteria.length) criteria.length = parseFloat(advancedCriteria.length);

    if (Object.keys(criteria).length > 0) {
      const fetchedHikes = await advancedSearchHikes(criteria);
      setResults(fetchedHikes);
      setHasSearched(true);
    }
  };

  const handleClearSearch = () => {
    setSearchTerm('');
    setAdvancedCriteria({ name: '', location: '', date: '', length: '' });
    setResults([]);
    setHasSearched(false);
  };

  const renderHikeItem = ({ item }) => (
    <Card
      style={styles.card}
      onPress={() => navigation.navigate('HikeDetails', { hike: item })}
    >
      <Card.Content>
        <Title style={styles.hikeName}>{item.name}</Title>
        <View style={styles.detailRow}>
          <Text style={styles.label}>Location:</Text>
          <Text style={styles.value}>{item.location}</Text>
        </View>
        <View style={styles.detailRow}>
          <Text style={styles.label}>Date:</Text>
          <Text style={styles.value}>{item.date}</Text>
        </View>
        <View style={styles.detailRow}>
          <Text style={styles.label}>Length:</Text>
          <Text style={styles.value}>{item.length} km</Text>
        </View>
        <View style={styles.detailRow}>
          <Text style={styles.label}>Difficulty:</Text>
          <Text style={styles.value}>{item.difficulty}</Text>
        </View>
      </Card.Content>
    </Card>
  );

  return (
    <View style={styles.container}>
      <Card style={styles.searchCard}>
        <Card.Content>
          <View style={styles.radioContainer}>
            <RadioButton.Group
              onValueChange={(value) => {
                setSearchMode(value);
                handleClearSearch();
              }}
              value={searchMode}
            >
              <View style={styles.radioRow}>
                <View style={styles.radioItem}>
                  <RadioButton value="simple" />
                  <Text>Simple Search</Text>
                </View>
                <View style={styles.radioItem}>
                  <RadioButton value="advanced" />
                  <Text>Advanced Search</Text>
                </View>
              </View>
            </RadioButton.Group>
          </View>

          <Divider style={styles.divider} />

          {searchMode === 'simple' ? (
            <>
              <TextInput
                label="Search by Hike Name"
                value={searchTerm}
                onChangeText={setSearchTerm}
                mode="outlined"
                style={styles.input}
                placeholder="Enter hike name..."
              />
              <View style={styles.buttonRow}>
                <Button
                  mode="contained"
                  onPress={handleSimpleSearch}
                  style={styles.searchButton}
                  icon="magnify"
                >
                  Search
                </Button>
                <Button
                  mode="outlined"
                  onPress={handleClearSearch}
                  style={styles.clearButton}
                >
                  Clear
                </Button>
              </View>
            </>
          ) : (
            <>
              <TextInput
                label="Hike Name"
                value={advancedCriteria.name}
                onChangeText={(text) =>
                  setAdvancedCriteria({ ...advancedCriteria, name: text })
                }
                mode="outlined"
                style={styles.input}
                placeholder="Optional"
              />
              <TextInput
                label="Location"
                value={advancedCriteria.location}
                onChangeText={(text) =>
                  setAdvancedCriteria({ ...advancedCriteria, location: text })
                }
                mode="outlined"
                style={styles.input}
                placeholder="Optional"
              />
              <TextInput
                label="Date (YYYY-MM-DD)"
                value={advancedCriteria.date}
                onChangeText={(text) =>
                  setAdvancedCriteria({ ...advancedCriteria, date: text })
                }
                mode="outlined"
                style={styles.input}
                placeholder="Optional"
              />
              <TextInput
                label="Length (km)"
                value={advancedCriteria.length}
                onChangeText={(text) =>
                  setAdvancedCriteria({ ...advancedCriteria, length: text })
                }
                mode="outlined"
                style={styles.input}
                keyboardType="decimal-pad"
                placeholder="Optional"
              />
              <View style={styles.buttonRow}>
                <Button
                  mode="contained"
                  onPress={handleAdvancedSearch}
                  style={styles.searchButton}
                  icon="magnify"
                >
                  Search
                </Button>
                <Button
                  mode="outlined"
                  onPress={handleClearSearch}
                  style={styles.clearButton}
                >
                  Clear
                </Button>
              </View>
            </>
          )}
        </Card.Content>
      </Card>

      {hasSearched && (
        <View style={styles.resultsContainer}>
          <Text style={styles.resultsText}>
            {results.length} {results.length === 1 ? 'result' : 'results'} found
          </Text>
          {results.length === 0 ? (
            <Text style={styles.noResultsText}>
              No hikes match your search criteria
            </Text>
          ) : (
            <FlatList
              data={results}
              renderItem={renderHikeItem}
              keyExtractor={(item) => item.hikeId.toString()}
              contentContainerStyle={styles.list}
            />
          )}
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  searchCard: {
    margin: 15,
    elevation: 4,
  },
  radioContainer: {
    marginBottom: 10,
  },
  radioRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-around',
  },
  radioItem: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  divider: {
    marginVertical: 10,
  },
  input: {
    marginBottom: 10,
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 10,
  },
  searchButton: {
    flex: 1,
    marginRight: 5,
    backgroundColor: '#2e7d32',
  },
  clearButton: {
    flex: 1,
    marginLeft: 5,
  },
  resultsContainer: {
    flex: 1,
    padding: 15,
  },
  resultsText: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 10,
  },
  noResultsText: {
    fontSize: 14,
    color: '#666',
    textAlign: 'center',
    marginTop: 20,
  },
  list: {
    paddingBottom: 20,
  },
  card: {
    marginBottom: 15,
    elevation: 3,
  },
  hikeName: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#2e7d32',
    marginBottom: 8,
  },
  detailRow: {
    flexDirection: 'row',
    marginBottom: 5,
  },
  label: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#333',
    width: 80,
  },
  value: {
    fontSize: 14,
    color: '#555',
    flex: 1,
  },
});

export default SearchHikeScreen;
