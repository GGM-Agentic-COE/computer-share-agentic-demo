# ComputerShare Agentic Demo (SEC Form 4)

## Installation
cd api && mvn spring-boot:run 

cd ui && npm install && npm run dev


## How to run application: 

1.	Start as the executive. Land on Executive Home, logged in as J. Alvarez. Narrate: "In production this page is reached via corporate SSO — no separate login exists, by design."
2.	Trigger the trade. Click Execute Trade. Narrate: "This stands in for a real EquatePlus transaction event — in production it fires automatically the instant a trade executes." Point out the new row appears immediately with status Validated, and that the form behind it isn't just shares and price — it's the full SEC Form 4 field set, pre-filled with zero manual entry.
3.	Switch persona to Legal & Compliance (dropdown, top right — no page reload, this simulates changing who's logged in). Go to Compliance Dashboard. Narrate: "Every executive's filings, in one place, sorted by whichever is closest to its actual EDGAR deadline — not creation date. Nobody's hunting for what needs attention." Show the status filter and search.
4.	Open the filing. Click the row → Filing Review. Scroll the five sections — Reporting Person, Issuer, Relationship to Issuer, Transaction Details, Signature. Narrate: "Every field the SEC actually requires is here, and every one of them is editable — not just a couple of numbers."
5.	Show the validation control. Edit Shares to 0, click Save Edits → inline error appears, Approve & Submit stays disabled. Narrate: "The system won't let an invalid filing reach a regulator — enforced twice, once in the browser and again on the server, so it can't be bypassed." Fix the value back, save again.
6.	Sign it. Type a name into Signature of Reporting Person, click Save Edits. Narrate: "Nothing gets approved without a named person attesting to it — the same pattern a real filer uses on an actual EDGAR submission. Notice Approve & Submit was still disabled until this happened."
7.	Download the actual document. Click Download PDF, open the file. Narrate: "This isn't a mockup — it's a field-for-field replica of the real SEC Form 4, generated fresh from this exact data."
8.	Approve. Click Approve & Submit → status flips to Submitted. Click Show audit trail → the full GENERATED → EDITED → APPROVED → SUBMITTED chain with actor IDs and timestamps. Narrate: "This is the record a regulator or internal auditor would ask for — who did what, when, immutable."
9.	Close the loop for the executive. Switch persona back to J. Alvarez, open the notification bell → the submission confirmation is there. Narrate: "The executive gets proof of compliance without having to ask legal for a status update."
