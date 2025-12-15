import React from 'react';
import { View, StyleSheet, ScrollView, Alert } from 'react-native';
import { Card, Title, Text, Button, Divider, IconButton } from 'react-native-paper';

const HikeDetailsScreen = ({ navigation, route }) => {
  const { hike = {} } = route?.params || {};

  const handleEdit = () => {
    navigation.navigate('AddHike', { hike });
  };

  const handleStartHiking = () => {
    // Will be implemented later
    Alert.alert('Start Hiking', 'This feature will allow you to start tracking your hike');
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { 
      month: 'short', 
      day: 'numeric', 
      year: 'numeric' 
    });
  };

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <IconButton
          icon="arrow-left"
          iconColor="#fff"
          size={24}
          onPress={() => navigation.goBack()}
          style={styles.backButton}
        />
        <View style={styles.headerText}>
          <Text style={styles.headerTitle}>Hiking Details</Text>
          <Text style={styles.headerSubtitle}>View hiking information</Text>
        </View>
      </View>

      <ScrollView style={styles.scrollView}>
        {/* Main Info Card */}
        <Card style={styles.mainCard}>
          <Card.Content>
            <Title style={styles.hikeName}>{hike.name}</Title>
            <View style={styles.locationRow}>
              <Text style={styles.locationIcon}>📍</Text>
              <Text style={styles.locationText}>{hike.location}</Text>
            </View>
          </Card.Content>
        </Card>

        {/* Details Grid */}
        <View style={styles.gridContainer}>
          {/* Hike ID Card */}
          <Card style={styles.gridCard}>
            <Card.Content style={styles.gridCardContent}>
              <View style={styles.gridIconRow}>
                <Text style={styles.gridIcon}>#</Text>
                <Text style={styles.gridLabel}>Hike ID</Text>
              </View>
              <Text style={styles.gridValue}>{hike.hikeId}</Text>
            </Card.Content>
          </Card>

          {/* Date Card */}
          <Card style={styles.gridCard}>
            <Card.Content style={styles.gridCardContent}>
              <View style={styles.gridIconRow}>
                <Text style={styles.gridIcon}>📅</Text>
                <Text style={styles.gridLabel}>Date</Text>
              </View>
              <Text style={styles.gridValue}>{formatDate(hike.date)}</Text>
            </Card.Content>
          </Card>

          {/* Length Card */}
          <Card style={styles.gridCard}>
            <Card.Content style={styles.gridCardContent}>
              <View style={styles.gridIconRow}>
                <Text style={styles.gridIcon}>📏</Text>
                <Text style={styles.gridLabel}>Length</Text>
              </View>
              <Text style={styles.gridValue}>{hike.length} km</Text>
            </Card.Content>
          </Card>

          {/* Difficulty Card */}
          <Card style={styles.gridCard}>
            <Card.Content style={styles.gridCardContent}>
              <View style={styles.gridIconRow}>
                <Text style={styles.gridIcon}>📈</Text>
                <Text style={styles.gridLabel}>Difficulty</Text>
              </View>
              <Text style={styles.gridValue}>{hike.difficulty}</Text>
            </Card.Content>
          </Card>
        </View>

        {/* Parking Availability Card */}
        <Card style={styles.parkingCard}>
          <Card.Content style={styles.parkingContent}>
            <View style={styles.parkingLeft}>
              <View style={styles.parkingIconContainer}>
                <Text style={styles.parkingIcon}>🚗</Text>
              </View>
              <View>
                <Text style={styles.parkingTitle}>Parking</Text>
                <Text style={styles.parkingSubtitle}>Availability status</Text>
              </View>
            </View>
            <View style={[
              styles.parkingBadge,
              { backgroundColor: (hike.parkingAvailable === 1 || hike.parkingAvailable === true) ? '#10b981' : '#9ca3af' }
            ]}>
              <Text style={styles.parkingBadgeText}>
                {(hike.parkingAvailable === 1 || hike.parkingAvailable === true) ? 'Available' : 'Not Available'}
              </Text>
            </View>
          </Card.Content>
        </Card>

        {/* Description Card */}
        {hike.description && (
          <Card style={styles.descriptionCard}>
            <Card.Content>
              <View style={styles.descriptionHeader}>
                <Text style={styles.descriptionIcon}>📝</Text>
                <Text style={styles.descriptionTitle}>Description</Text>
              </View>
              <Text style={styles.descriptionText}>{hike.description}</Text>
            </Card.Content>
          </Card>
        )}

        {/* Action Buttons */}
        <View style={styles.buttonContainer}>
          <Button
            mode="contained"
            onPress={handleStartHiking}
            style={styles.startButton}
            buttonColor="#10b981"
            textColor="#fff"
          >
            Start This Hike
          </Button>
          <Button
            mode="outlined"
            onPress={handleEdit}
            style={styles.editButton}
            textColor="#10b981"
          >
            Edit Details
          </Button>
          <Button
            mode="text"
            onPress={() => navigation.goBack()}
            style={styles.backTextButton}
            textColor="#059669"
          >
            Back to List
          </Button>
        </View>
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    backgroundColor: '#10b981',
    paddingVertical: 16,
    paddingHorizontal: 24,
    flexDirection: 'row',
    alignItems: 'center',
  },
  backButton: {
    margin: 0,
  },
  headerText: {
    marginLeft: 8,
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#fff',
  },
  headerSubtitle: {
    fontSize: 14,
    color: '#fff',
    opacity: 0.9,
  },
  scrollView: {
    flex: 1,
  },
  mainCard: {
    margin: 15,
    marginBottom: 10,
    elevation: 2,
    borderRadius: 16,
    backgroundColor: '#fff',
  },
  hikeName: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#047857',
    marginBottom: 8,
  },
  locationRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  locationIcon: {
    fontSize: 16,
    marginRight: 8,
  },
  locationText: {
    fontSize: 16,
    color: '#6b7280',
  },
  gridContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: 15,
    gap: 10,
  },
  gridCard: {
    width: '48%',
    elevation: 2,
    borderRadius: 12,
    backgroundColor: '#d1fae5',
    borderColor: '#a7f3d0',
    borderWidth: 1,
  },
  gridCardContent: {
    paddingVertical: 16,
  },
  gridIconRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  gridIcon: {
    fontSize: 16,
    marginRight: 8,
  },
  gridLabel: {
    fontSize: 12,
    color: '#6b7280',
  },
  gridValue: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#047857',
  },
  parkingCard: {
    margin: 15,
    marginTop: 10,
    elevation: 2,
    borderRadius: 12,
    backgroundColor: '#fff',
  },
  parkingContent: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  parkingLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  parkingIconContainer: {
    backgroundColor: '#d1fae5',
    padding: 12,
    borderRadius: 50,
    marginRight: 12,
  },
  parkingIcon: {
    fontSize: 20,
  },
  parkingTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#047857',
  },
  parkingSubtitle: {
    fontSize: 12,
    color: '#6b7280',
  },
  parkingBadge: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
  },
  parkingBadgeText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: 'bold',
  },
  descriptionCard: {
    margin: 15,
    marginTop: 0,
    elevation: 2,
    borderRadius: 12,
    backgroundColor: '#fff',
  },
  descriptionHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  descriptionIcon: {
    fontSize: 20,
    marginRight: 8,
  },
  descriptionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#047857',
  },
  descriptionText: {
    fontSize: 14,
    color: '#374151',
    lineHeight: 22,
  },
  buttonContainer: {
    padding: 15,
    paddingTop: 10,
    paddingBottom: 30,
  },
  startButton: {
    paddingVertical: 8,
    marginBottom: 10,
  },
  editButton: {
    borderColor: '#10b981',
    marginBottom: 10,
  },
  backTextButton: {
    backgroundColor: '#d1fae5',
  },
});

export default HikeDetailsScreen;
