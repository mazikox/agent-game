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
  'Forest Wolf': require('../../../assets/monster_avatar.png'),
  'Giant Spider': require('../../../assets/monster_avatar.png'),
  'Shadowfang Dragon': require('../../../assets/monster_avatar.png'),
  'Spruce Tree': require('../../../assets/mobs/choinka.png'),
  'Spruce Tree (Alt)': require('../../../assets/mobs/choinkaINT.png'),
};

const getMonsterIcon = (name) => {
  return MONSTER_ICONS[name] || require('../../../assets/monster_avatar.png');
};

export const AdminPanel = ({ location, selectedX, selectedY, onClose }) => {
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState(null);
  const [wanderRadius, setWanderRadius] = useState('0');
  const [respawnSeconds, setRespawnSeconds] = useState('60');
  const [quickSpawn, setQuickSpawn] = useState(false);

  useEffect(() => {
    fetchTemplates();
  }, []);

  // Quick Spawn Logic: Auto-save when coordinates change
  useEffect(() => {
    if (quickSpawn && selectedX !== null && selectedY !== null && selectedTemplate && !loading) {
      handleSaveSpawn();
    }
  }, [selectedX, selectedY]);

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

  const handleSaveSpawn = async () => {
    if (!selectedTemplate) {
      if (!quickSpawn) Alert.alert("Błąd", "Wybierz potwora.");
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

      if (!quickSpawn) {
        Alert.alert("Sukces", "Punkt respawnu został utworzony!");
      }
      // Keep selected template for next quick spawn
    } catch (error) {
      console.error("Failed to create spawn point:", error);
      if (!quickSpawn) Alert.alert("Błąd", "Nie udało się utworzyć punktu respawnu.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <View style={styles.headerTitleRow}>
          <Shield size={18} color={theme.colors.accent} />
          <Text style={styles.headerTitle}>ADMIN PANEL</Text>
        </View>
        <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
          <X size={20} color="#a0aec0" />
        </TouchableOpacity>
      </View>

      <ScrollView style={styles.content} showsVerticalScrollIndicator={false}>
        {/* Quick Spawn Toggle */}
        <TouchableOpacity 
          style={styles.quickSpawnRow} 
          onPress={() => setQuickSpawn(!quickSpawn)}
        >
          <View style={[styles.checkbox, quickSpawn && styles.checkboxActive]}>
            {quickSpawn && <View style={styles.checkboxInner} />}
          </View>
          <Text style={styles.quickSpawnLabel}>QUICK SPAWN (AUTO-SAVE)</Text>
        </TouchableOpacity>

        {selectedX !== null ? (
          <>
            <View style={styles.coordBadge}>
              <Text style={styles.coordText}>CEL: {selectedX}, {selectedY}</Text>
            </View>

            <Text style={styles.label}>Wybierz potwora:</Text>
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
                  <Image source={getMonsterIcon(t.name)} style={styles.monsterThumb} />
                  <View style={styles.templateInfo}>
                    <Text style={[
                      styles.templateName,
                      selectedTemplate?.id === t.id && styles.selectedTemplateName
                    ]}>{t.name}</Text>
                    <Text style={styles.templateMeta}>Lvl {t.level}</Text>
                  </View>
                </TouchableOpacity>
              ))}
            </View>

            <Text style={styles.label}>Wander Radius:</Text>
            <TextInput
              style={styles.input}
              value={wanderRadius}
              onChangeText={setWanderRadius}
              keyboardType="numeric"
              placeholder="np. 0"
              placeholderTextColor="#718096"
            />

            <Text style={styles.label}>Respawn (sek):</Text>
            <TextInput
              style={styles.input}
              value={respawnSeconds}
              onChangeText={setRespawnSeconds}
              keyboardType="numeric"
              placeholder="np. 60"
              placeholderTextColor="#718096"
            />

            {!quickSpawn && (
              <TouchableOpacity 
                style={[styles.saveButton, loading && { opacity: 0.7 }]}
                onPress={handleSaveSpawn}
                disabled={loading}
              >
                <Plus size={18} color="#fff" style={{ marginRight: 6 }} />
                <Text style={styles.saveButtonText}>Zapisz Respawn</Text>
              </TouchableOpacity>
            )}
          </>
        ) : (
          <View style={styles.emptyState}>
            <Text style={styles.emptyText}>KLIKNIJ NA MAPĘ,</Text>
            <Text style={styles.emptyTextSub}>ABY WYBRAĆ PUNKT</Text>
          </View>
        )}
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    width: 200,
    backgroundColor: 'rgba(15, 15, 18, 0.95)',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
    borderRadius: 12,
    padding: 12,
    maxHeight: 600, // Zwiększona wysokość
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.5,
    shadowRadius: 10,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 10,
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(255, 255, 255, 0.05)',
    paddingBottom: 8,
  },
  headerTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  headerTitle: {
    color: theme.colors.accent,
    fontSize: 14,
    fontFamily: theme.typography.fantasy,
    letterSpacing: 1,
  },
  closeBtn: {
    padding: 4,
  },
  content: {
    flex: 1,
  },
  quickSpawnRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
    gap: 8,
    backgroundColor: 'rgba(255, 255, 255, 0.03)',
    padding: 8,
    borderRadius: 6,
  },
  checkbox: {
    width: 16,
    height: 16,
    borderWidth: 1.5,
    borderColor: '#4a5568',
    borderRadius: 4,
    justifyContent: 'center',
    alignItems: 'center',
  },
  checkboxActive: {
    borderColor: theme.colors.accent,
    backgroundColor: 'rgba(246, 173, 85, 0.2)',
  },
  checkboxInner: {
    width: 8,
    height: 8,
    backgroundColor: theme.colors.accent,
    borderRadius: 1,
  },
  quickSpawnLabel: {
    color: '#cbd5e0',
    fontSize: 9,
    fontWeight: 'bold',
  },
  coordBadge: {
    backgroundColor: 'rgba(246, 173, 85, 0.1)',
    paddingVertical: 4,
    paddingHorizontal: 8,
    borderRadius: 4,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: 'rgba(246, 173, 85, 0.3)',
  },
  coordText: {
    color: theme.colors.accent,
    fontSize: 12,
    fontWeight: 'bold',
    textAlign: 'center',
  },
  label: {
    color: '#a0aec0',
    fontSize: 10,
    fontWeight: 'bold',
    marginBottom: 4,
    textTransform: 'uppercase',
  },
  input: {
    backgroundColor: 'rgba(255, 255, 255, 0.05)',
    color: '#fff',
    borderRadius: 6,
    padding: 8,
    fontSize: 12,
    marginBottom: 10,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.1)',
  },
  templateList: {
    gap: 6,
    marginBottom: 10,
  },
  templateItem: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(255, 255, 255, 0.03)',
    borderRadius: 6,
    padding: 6,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
    gap: 8,
  },
  selectedTemplateItem: {
    borderColor: theme.colors.accent,
    backgroundColor: 'rgba(246, 173, 85, 0.1)',
  },
  monsterThumb: {
    width: 24,
    height: 24,
    borderRadius: 12,
  },
  templateInfo: {
    flex: 1,
  },
  templateName: {
    color: '#fff',
    fontSize: 11,
    fontWeight: 'bold',
  },
  selectedTemplateName: {
    color: theme.colors.accent,
  },
  templateMeta: {
    color: '#718096',
    fontSize: 9,
  },
  saveButton: {
    backgroundColor: '#2f855a',
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 10,
    borderRadius: 6,
    marginTop: 5,
    marginBottom: 10,
  },
  saveButtonText: {
    color: '#fff',
    fontSize: 13,
    fontWeight: 'bold',
  },
  emptyState: {
    paddingVertical: 30,
    alignItems: 'center',
  },
  emptyText: {
    color: '#718096',
    fontSize: 12,
    fontWeight: 'bold',
  },
  emptyTextSub: {
    color: '#4a5568',
    fontSize: 10,
    marginTop: 4,
  }
});
