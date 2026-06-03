import random, datetime

with open('seed_100_operations.sql', 'w', encoding='utf-8') as f:
    
    dates = [datetime.date(2026, 1, 1) + datetime.timedelta(days=random.randint(0, 150)) for _ in range(100)]
    dates.sort()
    
    for i, d in enumerate(dates):
        # Répartition des natures: 55% Encaissement, 25% Décaissement, 10% Créance, 10% Dette
        rand_nature = random.random()
        if rand_nature < 0.55:
            nature = 'Encaissement'
        elif rand_nature < 0.80:
            nature = 'Décaissement'
        elif rand_nature < 0.90:
            nature = 'Créance'
        else:
            nature = 'Dette'
            
        # Répartition des états: 80% Validé, 10% En attente, 10% Annulé
        rand_etat = random.random()
        if rand_etat < 0.80:
            etat = 'Validé'
        elif rand_etat < 0.90:
            etat = 'En attente'
        else:
            etat = 'Annulé'

        caisse = random.choice(['Orienet', 'Bestmobile', 'Versus Info', '4S Mobile', 'Versus Com', 'MH Best'])
        
        # Logique par nature
        if nature in ['Encaissement', 'Créance']:
            titulaire = random.choice(['Client B2B', 'Client Boutique', 'Client Web', 'Grossiste Local', 'Partenaire IT'])
            famille = 'Vente'
            designation = 'Facture Vente'
            desc = 'Prestation de service ou vente de marchandises'
            ccp = 'Recettes' if nature == 'Encaissement' else 'Facture Emise'
            mode = random.choice(['Virement', 'Espèces', 'Chèque', 'Carte Bancaire']) if nature == 'Encaissement' else 'En attente'
        else: # Décaissement ou Dette
            titulaire = random.choice(['Fournisseur Tech', 'Agence Marketing', 'Fournisseur Bureautique', 'Bailleur', 'STE Télécom'])
            famille = random.choice(['Charge', 'Investissement', 'Fourniture', 'Salaire'])
            if famille == 'Charge':
                designation = 'Paiement Facture'
                desc = 'Facture électricité ou internet'
            elif famille == 'Investissement':
                designation = 'Achat Matériel'
                desc = 'Renouvellement parc informatique'
            elif famille == 'Salaire':
                designation = 'Paiement Salaires'
                desc = 'Salaires du mois'
            else:
                designation = 'Achat Fourniture'
                desc = 'Fournitures de bureau'
                
            ccp = 'Dépenses' if nature == 'Décaissement' else 'Facture Reçue'
            mode = random.choice(['Virement', 'Espèces', 'Chèque']) if nature == 'Décaissement' else 'En attente'

        montant = round(random.uniform(100.00, 25000.00), 2)
        
        sql = f"INSERT INTO operation (Date_Operation, Nature_Flux, Caisse, Mode_Flux, Titulaire_Flux, montant, CC_P, Famille, Designation, Description, Etat, archivee, cree_par) VALUES ('{d.strftime('%Y-%m-%d')}', '{nature}', '{caisse}', '{mode}', '{titulaire}', {montant}, '{ccp}', '{famille}', '{designation}', '{desc}', '{etat}', 0, 'Youssef');\n"
        f.write(sql)

print("SQL script generated at seed_100_operations.sql")
