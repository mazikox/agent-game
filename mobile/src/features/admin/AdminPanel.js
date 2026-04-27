import React, { useState, useEffect } from 'react';
import { 
  View, 
  StyleSheet, 
  Text, 
  TouchableOpacity, 
  Modal, 
  TextInput, 
  ScrollView,
  ActivityIndicator,
  Alert,
  Image
} from 'react-native';
import { theme } from '../../theme/theme';
import { MapView } from '../world/MapView';
import { Shield, X, Plus } from 'lucide-react-native';
import axios from 'axios';
import { API_BASE_URL } from '../../api/agentApi';
import { authService } from '../../api/authService';

const MONSTER_ICONS = {
  '/creatures/wolf.png': require('../../../assets/monster_avatar.png'),
  '/creatures/spider.png': require('../../../assets/monster_avatar.png'),
  '/creatures/dragon.png': require('../../../assets/monster_avatar.png'),
};

const getMonsterIcon = (iconUrl) => {
  return MONSTER_ICONS[iconUrl] || require('../../../assets/monster_avatar.png');
};

export const AdminPanel = ({ agent, location, creatures }) => {
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [selectedX, setSelectedX] = useState(0);
  const [selectedY, setSelectedY] = useState(0);
  const [selectedTemplate, setSelectedTemplate] = useState(null);
  const [wanderRadius, setWanderRadius] = useState('5');
  const [respawnSeconds, setRespawnSeconds] = useState('60');

  useEffect(() => {
    fetchTemplates();
  }, []);

  const fetchTemplates = async () => {
    try {
      setLoading(true);
      const token = authService.getToken();
      const response = await axios.get(`${API_BASE_URL}/admin/creature-templates`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      setTemplates(response.data);
    } catch (error) {
      console.error("Failed to fetch templates:", error);
      Alert.alert("Błąd", "Nie udało się pobrać szablonów potworów.");
    } finally {
      setLoading(false);
    }
  };

  const handleMapPress = (x, y) => {
    setSelectedX(x);
    setSelectedY(y);
    setModalVisible(true);
  };

  const handleSaveSpawn = async () => {
    if (!selectedTemplate) {
      Alert.alert("Błąd", "Wybierz potwora.");
      return;
    }

    try {
      setLoading(true);
      const token = authService.getToken();
      await axios.post(`${API_BASE_URL}/admin/spawn-points`, {
        templateId: selectedTemplate.id,
        locationId: location.id,
        centerX: selectedX,
        centerY: selectedY,
        wanderRadius: parseInt(wanderRadius, 10),
        respawnSeconds: parseInt(respawnSeconds, 10)
      }, {
        headers: { Authorization: `Bearer ${token}` }
      });

      Alert.alert("Sukces", "Punkt respawnu został utworzony!");
      setModalVisible(false);
      setSelectedTemplate(null);
    } catch (error) {
      console.error("Failed to create spawn point:", error);
      Alert.alert("Błąd", "Nie udało się utworzyć punktu respawnu.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <>
      <MapView 
        agentX={agent?.x || 0}
        agentY={agent?.y || 0}
        mapWidth={location ? location.width : 100}
        mapHeight={location ? location.height : 100}
        portals={location ? location.portals : []}
        creatures={creatures || []}
        locationName={location ? location.name : 'Unknown Realm'}
        agentName={agent?.name || 'Shadow-01'}
        onPress={handleMapPress}
      />

      {/* Floating Admin Badge */}
      <View style={styles.adminBadge}>
        <Shield size={16} color="#ef4444" style={{ marginRight: 6 }} />
        <Text style={styles.adminBadgeText}>TRYB ADMINISTRATORA (KLIKNIJ MAPĘ)</Text>
      </View>

      {/* Modal do tworzenia respawnu */}
      <Modal
        animationType="slide"
        transparent={true}
        visible={modalVisible}
        onRequestClose={() => setModalVisible(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Nowy Respawn ({selectedX}, {selectedY})</Text>
              <TouchableOpacity onPress={() => setModalVisible(false)}>
                <X size={24} color="#a0aec0" />
              </TouchableOpacity>
            </View>

            <ScrollView style={styles.modalForm}>
              <Text style={styles.label}>Wybierz potwora:</Text>
              {loading && <ActivityIndicator color={theme.colors.accent} style={{ margin: 10 }} />}
              
              <View style={styles.templateList}>
                {templates.map(t => (
                  <TouchableOpacity 
                    key={t.id} 
                    style={[
                      styles.templateItem, 
                      selectedTemplate?.id === t.id && styles.selectedTemplateItem
                    ]}
                    onPress={() => setSelectedTemplate(t)}
                  >
                    <Image 
                      source={getMonsterIcon(t.iconUrl)} 
                      style={styles.monsterThumb} 
                    />
                    <View>
                      <Text style={[
                        styles.templateName,
                        selectedTemplate?.id === t.id && styles.selectedTemplateName
                      ]}>{t.name}</Text>
                      <Text style={styles.templateMeta}>Lvl {t.level} | {t.rank}</Text>
                    </View>
                  </TouchableOpacity>
                ))}
              </View>

              <Text style={styles.label}>Promień wędrówki (Wander Radius):</Text>
              <TextInput
                style={styles.input}
                value={wanderRadius}
                onChangeText={setWanderRadius}
                keyboardType="numeric"
                placeholder="np. 5"
                placeholderTextColor="#718096"
              />

              <Text style={styles.label}>Czas respawnu (sekundy):</Text>
              <TextInput
                style={styles.input}
                value={respawnSeconds}
                onChangeText={setRespawnSeconds}
                keyboardType="numeric"
                placeholder="np. 60"
                placeholderTextColor="#718096"
              />

              <TouchableOpacity 
                style={styles.saveButton}
                onPress={handleSaveSpawn}
                disabled={loading}
              >
                <Plus size={20} color="#fff" style={{ marginRight: 8 }} />
                <Text style={styles.saveButtonText}>Zapisz Respawn</Text>
              </TouchableOpacity>
            </ScrollView>
          </View>
        </View>
      </Modal>
    </>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'rgba(10, 10, 11, 0.95)',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
    padding: 15,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 15,
    gap: 10,
  },
  headerTitle: {
    color: theme.colors.accent,
    fontSize: 20,
    fontFamily: theme.typography.fantasy,
  },
  content: {
    flex: 1,
    flexDirection: 'row',
    gap: 15,
  },
  mapContainer: {
    flex: 1,
    alignItems: 'center',
  },
  instructionText: {
    color: '#a0aec0',
    marginBottom: 10,
    textAlign: 'center',
    fontFamily: 'sans-serif',
    fontSize: 12,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.7)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContent: {
    width: '80%',
    maxHeight: '80%',
    backgroundColor: '#1a202c',
    borderRadius: 12,
    borderWidth: 1,
    borderColor: theme.colors.accent,
    padding: 20,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 15,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.1)',
    paddingBottom: 10,
  },
  modalTitle: {
    color: '#fff',
    fontSize: 18,
    fontFamily: theme.typography.fantasy,
  },
  modalForm: {
    flex: 1,
  },
  label: {
    color: '#cbd5e0',
    fontSize: 14,
    marginTop: 10,
    marginBottom: 5,
    fontWeight: 'bold',
  },
  input: {
    backgroundColor: '#2d3748',
    color: '#fff',
    borderRadius: 6,
    padding: 10,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: '#4a5568',
  },
  templateList: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
    marginBottom: 10,
  },
  templateItem: {
    backgroundColor: '#2d3748',
    borderRadius: 6,
    padding: 10,
    borderWidth: 1,
    borderColor: '#4a5568',
    minWidth: 140,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
  },
  monsterThumb: {
    width: 32,
    height: 32,
    borderRadius: 16,
  },
  selectedTemplateItem: {
    borderColor: theme.colors.accent,
    backgroundColor: 'rgba(246, 173, 85, 0.1)',
  },
  templateName: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 'bold',
  },
  selectedTemplateName: {
    color: theme.colors.accent,
  },
  templateMeta: {
    color: '#a0aec0',
    fontSize: 10,
    marginTop: 2,
  },
  saveButton: {
    backgroundColor: '#2f855a',
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 12,
    borderRadius: 6,
    marginTop: 20,
    marginBottom: 10,
  },
  saveButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  adminBadge: {
    position: 'absolute',
    top: 20,
    left: 20,
    backgroundColor: 'rgba(220, 38, 38, 0.9)',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 20,
    flexDirection: 'row',
    alignItems: 'center',
    zIndex: 1000,
    borderWidth: 1,
    borderColor: '#fca5a5',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.5,
    shadowRadius: 3,
  },
  adminBadgeText: {
    color: '#fff',
    fontSize: 11,
    fontWeight: 'bold',
    letterSpacing: 1,
  }
});
