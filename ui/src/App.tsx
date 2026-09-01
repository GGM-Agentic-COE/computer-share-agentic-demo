import { Route, Routes } from 'react-router-dom';
import { Layout } from './components/Layout';
import { ExecutiveHome } from './pages/ExecutiveHome';
import { ComplianceDashboard } from './pages/ComplianceDashboard';
import { FilingReview } from './pages/FilingReview';

export function App() {
  return (
    <Layout>
      <Routes>
        <Route path="/" element={<ExecutiveHome />} />
        <Route path="/dashboard" element={<ComplianceDashboard />} />
        <Route path="/filings/:id" element={<FilingReview />} />
      </Routes>
    </Layout>
  );
}
