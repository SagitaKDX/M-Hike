import React from 'react';
import { View, StyleSheet, ScrollView, Alert } from 'react-native';
import { Card, Title, Text, Button, Divider } from 'react-native-paper';
import { insertHike, updateHike } from '../database/DatabaseHelper';

const ConfirmHikeScreen = ({ navigation, route }) => {
  const { hikeData = {}, isEditing = false, hikeId = null } = route?.params || {};

  const handleConfirm = async () => {
    try {
      if (isEditing) {
        const result = await updateHike(hikeId, hikeData);
        if (result.success) {
          Alert.alert('Success', 'Hike updated successfully!', [
            { text: 'OK', onPress: () => navigation.navigate('Home') },
          ]);
        } else {
          Alert.alert('Error', 'Failed to update hike');
        }
      } else {
        const result = await insertHike(hikeData);
        if (result.success) {
          Alert.alert('Success', 'Hike saved successfully!', [
            { text: 'OK', onPress: () => navigation.navigate('Home') },
          ]);
        } else {
          Alert.alert('Error', 'Failed to save hike');
        }
      }
    } catch (error) {
      Alert.alert('Error', 'An error occurred: ' + error.message);
    }
  };

  const handleEdit = () => {
    navigation.goBack();
  };

  return (
    <ScrollView style={styles.container}>
      <Card style={styles.card}>
        <Card.Content>
          <Title style={styles.title}>
            {isEditing ? 'Confirm Hike Update' : 'Confirm Hike Details'}
          </Title>
          <Text style={styles.subtitle}>
            Please review the details below before saving
          </Text>

          <Divider style={styles.divider} />

          <View style={styles.detailRow}>
            <Text style={styles.label}>Hike ID:</Text>
            <Text style={styles.value}>{hikeData.hikeId}</Text>
          </View>

          <View style={styles.detailRow}>
            <Text style={styles.label}>Hike Name:</Text>
            <Text style={styles.value}>{hikeData.name}</Text>
          </View>

          <View style={styles.detailRow}>
            <Text style={styles.label}>Location:</Text>
            <Text style={styles.value}>{hikeData.location}</Text>
          </View>

          <View style={styles.detailRow}>
            <Text style={styles.label}>Date:</Text>
            <Text style={styles.value}>{hikeData.date}</Text>
          </View>

          <View style={styles.detailRow}>
            <Text style={styles.label}>Parking Available:</Text>
            <Text style={styles.value}>{hikeData.parkingAvailable ? 'Yes' : 'No'}</Text>
          </View>

          <View style={styles.detailRow}>
            <Text style={styles.label}>Length:</Text>
            <Text style={styles.value}>{hikeData.length} km</Text>
          </View>

          <View style={styles.detailRow}>
            <Text style={styles.label}>Difficulty:</Text>
            <Text style={styles.value}>{hikeData.difficulty}</Text>
          </View>

          {hikeData.description && (
            <View style={styles.detailRow}>
              <Text style={styles.label}>Description:</Text>
              <Text style={styles.value}>{hikeData.description}</Text>
            </View>
          )}

          <Divider style={styles.divider} />

          <View style={styles.buttonContainer}>
            <Button
              mode="contained"
              onPress={handleConfirm}
              style={styles.confirmButton}
              buttonColor="#10b981"
              textColor="#fff"
              icon="check-circle"
            >
              {isEditing ? 'Update' : 'Confirm & Save'}
            </Button>
            <Button
              mode="outlined"
              onPress={handleEdit}
              style={styles.editButton}
              textColor="#10b981"
              icon="pencil"
            >
              Edit Details
            </Button>
          </View>
        </Card.Content>
      </Card>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  card: {
    margin: 15,
    elevation: 4,
  },
  title: {
    fontSize: 22,
    fontWeight: 'bold',
    color: '#2e7d32',
    textAlign: 'center',
  },
  subtitle: {
    textAlign: 'center',
    color: '#666',
    marginTop: 5,
    marginBottom: 10,
  },
  divider: {
    marginVertical: 15,
  },
  detailRow: {
    marginBottom: 15,
  },
  label: {
    fontSize: 14,
    fontWeight: 'bold',
    color: '#333',
    marginBottom: 5,
  },
  value: {
    fontSize: 16,
    color: '#555',
  },
  buttonContainer: {
    marginTop: 10,
  },
  confirmButton: {
    paddingVertical: 8,
    marginBottom: 10,
  },
  editButton: {
    borderColor: '#10b981',
  },
});

export default ConfirmHikeScreen;
