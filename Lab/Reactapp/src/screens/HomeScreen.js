import React from 'react';
import { View, StyleSheet, ScrollView, Image } from 'react-native';
import { Button, Card, Title, Paragraph } from 'react-native-paper';
import { MaterialCommunityIcons } from '@expo/vector-icons';

const HomeScreen = ({ navigation }) => {
  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <MaterialCommunityIcons name="hiking" size={80} color="#2e7d32" />
        <Title style={styles.title}>M-Hike</Title>
        <Paragraph style={styles.subtitle}>Your Hiking Companion</Paragraph>
      </View>

      <View style={styles.cardContainer}>
        <Card style={styles.card} onPress={() => navigation.navigate('AddHike')}>
          <Card.Content style={styles.cardContent}>
            <MaterialCommunityIcons name="plus-circle" size={50} color="#2e7d32" />
            <Title style={styles.cardTitle}>Add New Hike</Title>
            <Paragraph style={styles.cardText}>
              Plan your next hiking adventure
            </Paragraph>
          </Card.Content>
        </Card>

        <Card style={styles.card} onPress={() => navigation.navigate('ViewHikes')}>
          <Card.Content style={styles.cardContent}>
            <MaterialCommunityIcons name="format-list-bulleted" size={50} color="#1976d2" />
            <Title style={styles.cardTitle}>View All Hikes</Title>
            <Paragraph style={styles.cardText}>
              Browse and manage your hikes
            </Paragraph>
          </Card.Content>
        </Card>

        <Card style={styles.card} onPress={() => navigation.navigate('SearchHike')}>
          <Card.Content style={styles.cardContent}>
            <MaterialCommunityIcons name="magnify" size={50} color="#f57c00" />
            <Title style={styles.cardTitle}>Search Hikes</Title>
            <Paragraph style={styles.cardText}>
              Find specific hikes quickly
            </Paragraph>
          </Card.Content>
        </Card>
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    alignItems: 'center',
    paddingVertical: 30,
    backgroundColor: '#fff',
    marginBottom: 20,
    elevation: 2,
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#2e7d32',
    marginTop: 10,
  },
  subtitle: {
    fontSize: 16,
    color: '#666',
    marginTop: 5,
  },
  cardContainer: {
    padding: 15,
  },
  card: {
    marginBottom: 15,
    elevation: 4,
    borderRadius: 10,
  },
  cardContent: {
    alignItems: 'center',
    paddingVertical: 25,
  },
  cardTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    marginTop: 10,
    textAlign: 'center',
  },
  cardText: {
    textAlign: 'center',
    color: '#666',
    marginTop: 5,
  },
});

export default HomeScreen;
