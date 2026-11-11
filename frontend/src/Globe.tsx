import { Canvas, useFrame } from '@react-three/fiber'
import { Float, Line, OrbitControls, Stars } from '@react-three/drei'
import { useMemo, useRef } from 'react'
import * as THREE from 'three'
import type { Node, Zone } from './api'

function OrbitalWorld({ zones, nodes, reducedMotion }: { zones: Zone[]; nodes: Node[]; reducedMotion: boolean }) {
  const globe = useRef<THREE.Group>(null)
  const points = useMemo(() => zones.map((zone, index) => {
    const phi = Math.acos(-1 + (2 * (index + 1)) / (zones.length + 1))
    const theta = Math.sqrt(zones.length * Math.PI) * phi
    const radius = 1.72
    return { zone, position: new THREE.Vector3(radius * Math.sin(phi) * Math.cos(theta), radius * Math.cos(phi), radius * Math.sin(phi) * Math.sin(theta)) }
  }), [zones])
  useFrame((state, delta) => {
    if (!globe.current || reducedMotion) return
    globe.current.rotation.y += delta * 0.055
    globe.current.rotation.x = THREE.MathUtils.damp(globe.current.rotation.x, state.pointer.y * 0.12, 2, delta)
  })
  return <group ref={globe}>
    <mesh>
      <sphereGeometry args={[1.7, 48, 48]} />
      <meshStandardMaterial color="#06142e" metalness={0.7} roughness={0.38} emissive="#06215a" emissiveIntensity={0.38} />
    </mesh>
    <mesh>
      <sphereGeometry args={[1.715, 32, 24]} />
      <meshBasicMaterial color="#08cfff" wireframe transparent opacity={0.14} />
    </mesh>
    <mesh>
      <sphereGeometry args={[1.735, 48, 32]} />
      <meshBasicMaterial color="#712dff" wireframe transparent opacity={0.08} />
    </mesh>
    {[1.9, 2.15, 2.4].map((r, i) => <mesh key={r} rotation={[Math.PI / 2 + i * 0.26, i * 0.36, i * 0.2]}>
      <torusGeometry args={[r, 0.008, 8, 120]} />
      <meshBasicMaterial color={i === 1 ? '#a855f7' : '#00d9ff'} transparent opacity={0.46} />
    </mesh>)}
    {points.map(({ zone, position }, i) => <group key={zone.id} position={position}>
      <Float speed={reducedMotion ? 0 : 1.1 + i * 0.16} floatIntensity={reducedMotion ? 0 : 0.12}>
        <mesh>
          <octahedronGeometry args={[0.105, 0]} />
          <meshStandardMaterial color={i % 2 ? '#ba64ff' : '#20e6ff'} emissive={i % 2 ? '#7627ff' : '#00b9ff'} emissiveIntensity={2.2} />
        </mesh>
      </Float>
      <mesh position={[0, -0.2, 0]}>
        <cylinderGeometry args={[0.012, 0.012, 0.3, 8]} />
        <meshBasicMaterial color="#12cfff" transparent opacity={0.65} />
      </mesh>
      <mesh position={[0, -0.36, 0]}>
        <boxGeometry args={[0.18, 0.14, 0.14]} />
        <meshStandardMaterial color="#14275d" emissive="#143caa" emissiveIntensity={1.4} />
      </mesh>
    </group>)}
    {points.length > 1 && points.map((point, i) => {
      const next = points[(i + 1) % points.length]
      return <Line key={`link-${point.zone.id}`} points={[point.position, new THREE.Vector3(0, 0, 0), next.position]} color="#00d9ff" transparent opacity={0.38} lineWidth={1} />
    })}
    {nodes.slice(0, 24).map((node, i) => {
      const angle = (i / Math.max(nodes.length, 1)) * Math.PI * 2
      const p = new THREE.Vector3(Math.cos(angle) * 2.15, Math.sin(angle * 1.7) * 0.22, Math.sin(angle) * 2.15)
      return <group key={node.id} position={p}>
        <mesh><boxGeometry args={[0.075, 0.13, 0.075]} /><meshStandardMaterial color={node.status === 'ONLINE' ? '#20dbff' : '#fb923c'} emissive={node.status === 'ONLINE' ? '#087aff' : '#f97316'} emissiveIntensity={2} /></mesh>
        <Line points={[p, p.clone().normalize().multiplyScalar(1.72)]} color={node.status === 'ONLINE' ? '#00cfff' : '#fb923c'} transparent opacity={0.5} lineWidth={1} />
      </group>
    })}
  </group>
}

export default function Globe({ zones, nodes, reducedMotion }: { zones: Zone[]; nodes: Node[]; reducedMotion: boolean }) {
  return <div className="globe-canvas" role="img" aria-label={`Visualização 3D da topologia: ${zones.length} zonas e ${nodes.length} nós`}>
    <Canvas camera={{ position: [0, 0, 6.5], fov: 42 }} dpr={[1, 1.5]} gl={{ alpha: true, antialias: true }}>
      <ambientLight intensity={0.9} />
      <pointLight position={[3, 3, 4]} color="#18baff" intensity={20} />
      <pointLight position={[-4, -1, -3]} color="#8f32ff" intensity={16} />
      <Stars radius={38} depth={22} count={reducedMotion ? 140 : 400} factor={2.2} fade speed={reducedMotion ? 0 : 0.12} />
      <OrbitalWorld zones={zones} nodes={nodes} reducedMotion={reducedMotion} />
      <OrbitControls enablePan={false} enableZoom={false} autoRotate={!reducedMotion} autoRotateSpeed={0.18} maxPolarAngle={Math.PI * 0.76} minPolarAngle={Math.PI * 0.24} />
    </Canvas>
    <div className="globe-scan" />
  </div>
}
