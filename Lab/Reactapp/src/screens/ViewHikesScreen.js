import React, { useState, useCallback } from 'react';
import { View, StyleSheet, FlatList, Alert } from 'react-native';
import { Card, Title, Text, Button, FAB, Menu, Divider } from 'react-native-paper';
import { useFocusEffect } from '@react-navigation/native';
import { getAllHikes, deleteHike, deleteAllHikes } from '../database/DatabaseHelper';

const ViewHikesScreen = ({ navigation }) => {
  const [hikes, setHikes] = useState([]);
  const [menuVisible, setMenuVisible] = useState(false);

  const loadHikes = async () => {
    const fetchedHikes = await getAllHikes();
    setHikes(fetchedHikes);
  };

  useFocusEffect(
    useCallback(() => {
      loadHikes();
    }, [])
  );

  const handleDeleteHike = (id, name) => {
    Alert.alert(
      'Delete Hike',
      `Are you sure you want to delete "${name}"?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: async () => {
            const result = await deleteHike(id);
            if (result.success) {
              Alert.alert('Success', 'Hike deleted successfully');
              loadHikes();
            } else {
              Alert.alert('Error', 'Failed to delete hike');
            }
          },
        },
      ]
    );
  };

  const handleDeleteAll = () => {
    Alert.alert(
      'Delete All Hikes',
      'Are you sure you want to delete ALL hikes? This action cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete All',
          style: 'destructive',
          onPress: async () => {
            const result = await deleteAllHikes();
            if (result.success) {
              Alert.alert('Success', 'All hikes deleted successfully');
              loadHikes();
              setMenuVisible(false);
            } else {
              Alert.alert('Error', 'Failed to delete hikes');
            }
          },
        },
      ]
    );
  };

  const renderHikeItem = ({ item }) => (
    <Card style={styles.card} onPress={() => navigation.navigate('HikeDetails', { hike: item })}>
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
          <Text style={[styles.value, styles.difficultyBadge]}>{item.difficulty}</Text>
        </View>
        <View style={styles.detailRow}>
          <Text style={styles.label}>Parking:</Text>
          <Text style={styles.value}>
            {(item.parkingAvailable === 1 || item.parkingAvailable === true) ? 'Available' : 'Not Available'}
          </Text>
        </View>
      </Card.Content>
      <Card.Actions>
        <Button onPress={() => navigation.navigate('HikeDetails', { hike: item })}>
          View Details
        </Button>
        <Button onPress={() => handleDeleteHike(item.hikeId, item.name)} textColor="#d32f2f">
          Delete
        </Button>
      </Card.Actions>
    </Card>
  );

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Menu
          visible={menuVisible}
          onDismiss={() => setMenuVisible(false)}
          anchor={
            <Button
              mode="outlined"
              onPress={() => setMenuVisible(true)}
              icon="menu"
              style={styles.menuButton}
            >
              Options
            </Button>
          }
        >
          <Menu.Item onPress={handleDeleteAll} title="Delete All Hikes" />
          <Divider />
          <Menu.Item onPress={() => setMenuVisible(false)} title="Cancel" />
        </Menu>
      </View>

      {hikes.length === 0 ? (
        <View style={styles.emptyContainer}>
          <Text style={styles.emptyText}>No hikes recorded yet</Text>
          <Button
            mode="contained"
            onPress={() => navigation.navigate('AddHike')}
            style={styles.addButton}
          >
            Add Your First Hike
          </Button>
        </View>
      ) : (
        <FlatList
          data={hikes}
          renderItem={renderHikeItem}
          keyExtractor={(item) => item.hikeId.toString()}
          contentContainerStyle={styles.list}
        />
      )}

      <FAB
        style={styles.fab}
        icon="plus"
        onPress={() => navigation.navigate('AddHike')}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    padding: 10,
    backgroundColor: '#fff',
    elevation: 2,
  },
  menuButton: {
    alignSelf: 'flex-end',
  },
  list: {
    padding: 15,
  },
  card: {
    marginBottom: 15,
    elevation: 3,
  },
  hikeName: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#2e7d32',
    marginBottom: 10,
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
  difficultyBadge: {
    fontWeight: 'bold',
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  emptyText: {
    fontSize: 18,
    color: '#666',
    marginBottom: 20,
  },
  addButton: {
    backgroundColor: '#2e7d32',
  },
  fab: {
    position: 'absolute',
    margin: 16,
    right: 0,
    bottom: 0,
    backgroundColor: '#2e7d32',
  },
});

export default ViewHikesScreen;
