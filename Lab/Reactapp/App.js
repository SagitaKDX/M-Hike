import React, { useEffect } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { Provider as PaperProvider } from 'react-native-paper';
import { initDatabase } from './src/database/DatabaseHelper';

import HomeScreen from './src/screens/HomeScreen';
import AddHikeScreen from './src/screens/AddHikeScreen';
import ConfirmHikeScreen from './src/screens/ConfirmHikeScreen';
import ViewHikesScreen from './src/screens/ViewHikesScreen';
import HikeDetailsScreen from './src/screens/HikeDetailsScreen';
import SearchHikeScreen from './src/screens/SearchHikeScreen';

const Stack = createNativeStackNavigator();

export default function App() {
  useEffect(() => {
    initDatabase();
  }, []);

  return (
    <PaperProvider>
      <NavigationContainer>
        <Stack.Navigator
          initialRouteName="Home"
          screenOptions={{
            headerStyle: {
              backgroundColor: '#2e7d32',
            },
            headerTintColor: '#fff',
            headerTitleStyle: {
              fontWeight: 'bold',
            },
          }}
        >
          <Stack.Screen 
            name="Home" 
            component={HomeScreen}
            options={{ title: 'M-Hike' }}
          />
          <Stack.Screen 
            name="AddHike" 
            component={AddHikeScreen}
            options={{ title: 'Add New Hike' }}
          />
          <Stack.Screen 
            name="ConfirmHike" 
            component={ConfirmHikeScreen}
            options={{ title: 'Confirm Hike Details' }}
          />
          <Stack.Screen 
            name="ViewHikes" 
            component={ViewHikesScreen}
            options={{ title: 'All Hikes' }}
          />
          <Stack.Screen 
            name="HikeDetails" 
            component={HikeDetailsScreen}
            options={{ title: 'Hike Details' }}
          />
          <Stack.Screen 
            name="SearchHike" 
            component={SearchHikeScreen}
            options={{ title: 'Search Hikes' }}
          />
        </Stack.Navigator>
      </NavigationContainer>
    </PaperProvider>
  );
}
