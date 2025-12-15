import React, { useState } from 'react';
import {
  View,
  StyleSheet,
  ScrollView,
  Platform,
  Alert,
  Switch,
} from 'react-native';
import {
  TextInput,
  Button,
  Text,
  HelperText,
  Card,
  IconButton,
} from 'react-native-paper';
import { Picker } from '@react-native-picker/picker';
import DateTimePicker from '@react-native-community/datetimepicker';

const AddHikeScreen = ({ navigation, route }) => {
  const isEditing = route?.params?.hike ? true : false;
  const existingHike = route?.params?.hike || {};

  const [formData, setFormData] = useState({
    hikeId: existingHike.hikeId || Date.now(),
    name: existingHike.name || '',
    location: existingHike.location || '',
    date: existingHike.date || new Date().toISOString().split('T')[0],
    parkingAvailable: existingHike.parkingAvailable === 1 || existingHike.parkingAvailable === true || false,
    length: existingHike.length?.toString() || '',
    difficulty: existingHike.difficulty || 'Easy',
    description: existingHike.description || '',
  });

  const [errors, setErrors] = useState({});
  const [showDatePicker, setShowDatePicker] = useState(false);
  const [selectedDate, setSelectedDate] = useState(
    existingHike.date ? new Date(existingHike.date) : new Date()
  );

  const validateForm = () => {
    const newErrors = {};

    if (!formData.name.trim()) {
      newErrors.name = 'Hike name is required';
    }

    if (!formData.location.trim()) {
      newErrors.location = 'Location is required';
    }

    if (!formData.date) {
      newErrors.date = 'Date is required';
    }

    if (!formData.length.trim()) {
      newErrors.length = 'Length is required';
    } else if (isNaN(parseFloat(formData.length)) || parseFloat(formData.length) <= 0) {
      newErrors.length = 'Length must be a positive number';
    }

    if (!formData.difficulty) {
      newErrors.difficulty = 'Difficulty level is required';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = () => {
    if (validateForm()) {
      navigation.navigate('ConfirmHike', {
        hikeData: {
          ...formData,
          length: parseFloat(formData.length),
        },
        isEditing,
        hikeId: existingHike.hikeId,
      });
    } else {
      Alert.alert('Validation Error', 'Please fill in all required fields correctly.');
    }
  };

  const handleDateChange = (event, date) => {
    setShowDatePicker(Platform.OS === 'ios');
    if (date) {
      setSelectedDate(date);
      setFormData({
        ...formData,
        date: date.toISOString().split('T')[0],
      });
    }
  };

  const updateField = (field, value) => {
    setFormData({ ...formData, [field]: value });
    if (errors[field]) {
      setErrors({ ...errors, [field]: '' });
    }
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <IconButton
          icon="arrow-left"
          iconColor="#fff"
          size={24}
          onPress={() => navigation.goBack()}
          style={styles.backButton}
        />
        <View style={styles.headerText}>
          <Text style={styles.headerTitle}>
            {isEditing ? 'Edit Hiking' : 'Add New Hiking'}
          </Text>
          <Text style={styles.headerSubtitle}>
            {isEditing ? 'Update hiking record' : 'Create a new hiking record'}
          </Text>
        </View>
      </View>

      <Card style={styles.card}>
        <Card.Content>
          <View style={styles.fieldContainer}>
            <Text style={styles.label}>
              Hike ID <Text style={styles.labelNote}>(Primary Key)</Text>
            </Text>
            <View style={styles.readOnlyField}>
              <Text style={styles.readOnlyText}>{formData.hikeId}</Text>
            </View>
          </View>

          <View style={styles.fieldContainer}>
            <Text style={styles.label}>
              <Text style={styles.iconText}>📝 </Text>Hiking Name *
            </Text>
            <TextInput
              value={formData.name}
              onChangeText={(text) => updateField('name', text)}
              mode="outlined"
              style={styles.input}
              error={!!errors.name}
              placeholder="e.g., Mountain Peak Trail"
              outlineColor="#10b981"
              activeOutlineColor="#059669"
            />
            <HelperText type="error" visible={!!errors.name}>
              {errors.name}
            </HelperText>
          </View>

          <View style={styles.fieldContainer}>
            <Text style={styles.label}>
              <Text style={styles.iconText}>📍 </Text>Location *
            </Text>
            <TextInput
              value={formData.location}
              onChangeText={(text) => updateField('location', text)}
              mode="outlined"
              style={styles.input}
              error={!!errors.location}
              placeholder="e.g., Rocky Mountain National Park"
              outlineColor="#10b981"
              activeOutlineColor="#059669"
            />
            <HelperText type="error" visible={!!errors.location}>
              {errors.location}
            </HelperText>
          </View>

          <View style={styles.fieldContainer}>
            <Text style={styles.label}>
              <Text style={styles.iconText}>📅 </Text>Date *
            </Text>
            <Button
              mode="outlined"
              onPress={() => setShowDatePicker(true)}
              style={styles.dateButton}
              icon="calendar"
              textColor="#059669"
            >
              {formData.date || 'Select Date'}
            </Button>
            {showDatePicker && (
              <DateTimePicker
                value={selectedDate}
                mode="date"
                display={Platform.OS === 'ios' ? 'spinner' : 'default'}
                onChange={handleDateChange}
              />
            )}
            <HelperText type="error" visible={!!errors.date}>
              {errors.date}
            </HelperText>
          </View>

          <View style={styles.fieldContainer}>
            <Text style={styles.label}>
              <Text style={styles.iconText}>📏 </Text>Length (km) *
            </Text>
            <TextInput
              value={formData.length}
              onChangeText={(text) => updateField('length', text)}
              mode="outlined"
              style={styles.input}
              keyboardType="decimal-pad"
              error={!!errors.length}
              placeholder="e.g., 8.5"
              outlineColor="#10b981"
              activeOutlineColor="#059669"
            />
            <HelperText type="error" visible={!!errors.length}>
              {errors.length}
            </HelperText>
          </View>

          <View style={styles.fieldContainer}>
            <Text style={styles.label}>
              <Text style={styles.iconText}>📈 </Text>Difficulty Level *
            </Text>
            <View style={styles.pickerWrapper}>
              <Picker
                selectedValue={formData.difficulty}
                onValueChange={(value) => updateField('difficulty', value)}
                style={styles.picker}
              >
                <Picker.Item label="Easy" value="Easy" />
                <Picker.Item label="Medium" value="Medium" />
                <Picker.Item label="Hard" value="Hard" />
                <Picker.Item label="Expert" value="Expert" />
              </Picker>
            </View>
            <HelperText type="error" visible={!!errors.difficulty}>
              {errors.difficulty}
            </HelperText>
          </View>

          <View style={styles.fieldContainer}>
            <View style={styles.switchContainer}>
              <View style={styles.switchLabel}>
                <Text style={styles.iconText}>🚗 </Text>
                <Text style={styles.label}>Parking Available</Text>
              </View>
              <Switch
                value={formData.parkingAvailable}
                onValueChange={(value) => updateField('parkingAvailable', value)}
                trackColor={{ false: '#d1d5db', true: '#86efac' }}
                thumbColor={formData.parkingAvailable ? '#10b981' : '#f3f4f6'}
              />
            </View>
          </View>

          <View style={styles.fieldContainer}>
            <Text style={styles.label}>Description</Text>
            <TextInput
              value={formData.description}
              onChangeText={(text) => updateField('description', text)}
              mode="outlined"
              style={styles.input}
              multiline
              numberOfLines={5}
              placeholder="Add details about the trail, scenery, or tips..."
              outlineColor="#10b981"
              activeOutlineColor="#059669"
            />
          </View>

          <View style={styles.buttonContainer}>
            <Button
              mode="contained"
              onPress={handleSubmit}
              style={styles.submitButton}
              buttonColor="#10b981"
              textColor="#fff"
            >
              {isEditing ? 'Update Hiking' : 'Save Hiking'}
            </Button>
            <Button
              mode="outlined"
              onPress={() => navigation.goBack()}
              style={styles.cancelButton}
              textColor="#059669"
            >
              Cancel
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
  card: {
    margin: 15,
    elevation: 4,
  },
  fieldContainer: {
    marginBottom: 16,
  },
  label: {
    fontSize: 14,
    color: '#047857',
    marginBottom: 8,
    fontWeight: '500',
  },
  labelNote: {
    fontSize: 12,
    color: '#6b7280',
    fontWeight: 'normal',
  },
  iconText: {
    fontSize: 16,
  },
  readOnlyField: {
    backgroundColor: '#d1fae5',
    borderWidth: 1,
    borderColor: '#a7f3d0',
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  readOnlyText: {
    fontSize: 16,
    color: '#6b7280',
  },
  input: {
    backgroundColor: '#fff',
  },
  dateButton: {
    borderColor: '#10b981',
  },
  pickerWrapper: {
    borderWidth: 1,
    borderColor: '#10b981',
    borderRadius: 5,
    backgroundColor: '#fff',
  },
  picker: {
    height: 50,
  },
  switchContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
  },
  switchLabel: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  buttonContainer: {
    marginTop: 20,
    marginBottom: 10,
  },
  submitButton: {
    paddingVertical: 8,
    marginBottom: 10,
  },
  cancelButton: {
    borderColor: '#10b981',
  },
});

export default AddHikeScreen;
